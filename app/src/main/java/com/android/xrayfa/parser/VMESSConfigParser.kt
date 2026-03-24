package com.android.xrayfa.parser

import com.android.xrayfa.XrayAppCompatFactory
import com.android.xrayfa.common.GEO_LITE
import com.android.xrayfa.common.repository.SettingsRepository
import com.android.xrayfa.dto.Link
import com.android.xrayfa.dto.Node
import com.android.xrayfa.dto.VMESSConfig
import com.android.xrayfa.model.OutboundObject
import com.android.xrayfa.model.ServerObject
import com.android.xrayfa.model.UserObject
import com.android.xrayfa.model.VMESSOutboundConfigurationObject
import com.android.xrayfa.model.protocol.Protocol
import com.android.xrayfa.model.stream.GrpcSettings
import com.android.xrayfa.model.stream.HttpHeaderObject
import com.android.xrayfa.model.stream.HttpRequestObject
import com.android.xrayfa.model.stream.KcpHeaderObject
import com.android.xrayfa.model.stream.KcpSettings
import com.android.xrayfa.model.stream.RawSettings
import com.android.xrayfa.model.stream.StreamSettingsObject
import com.android.xrayfa.model.stream.TlsSettings
import com.android.xrayfa.model.stream.WsSettings
import com.android.xrayfa.utils.Device
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VMESSConfigParser
@Inject constructor(
    override val settingsRepo: SettingsRepository
): AbstractConfigParser<VMESSOutboundConfigurationObject>() {

    companion object {
        const val TAG = "VMESSConfigParser"

        fun decodeVMESS(url: String): VMESSConfig {
            val cleanLink = url.removePrefix("vmess://").trim()
            val decoded = String(Base64.getDecoder().decode(cleanLink))
            val json = JsonParser.parseString(decoded).asJsonObject
            val uuid = json.get("id").asString
            val tls = json.get("tls")?.asString ?: ""
            val host = json.get("host")?.asString ?: ""
            val network = json.get("net")?.asString ?: "tcp"
            val address = json.get("add").asString
            return VMESSConfig(
                uuid = uuid,
                tls = tls,
                host = host,
                network = network,
                address = address,
                others = json
            )
        }

        fun encodeVMESS(config: VMESSConfig): String {
            val json = config.others.deepCopy()
            json.addProperty("v", "2")
            json.addProperty("id", config.uuid)
            json.addProperty("tls", config.tls)
            json.addProperty("host", config.host)
            json.addProperty("net", config.network)
            json.addProperty("add", config.address)
            
            val jsonString = json.toString()
            val encoded = Base64.getEncoder().encodeToString(jsonString.toByteArray())
            return "vmess://$encoded"
        }
    }

    override fun parseOutbound(url: String): OutboundObject<VMESSOutboundConfigurationObject> {
        try {
            val vmess = decodeVMESS(url)
            val uuid = vmess.uuid
            val tls = vmess.tls
            val host = vmess.host
            val network = vmess.network
            val address = vmess.address
            val json = vmess.others
            return OutboundObject(
                protocol = "vmess",
                settings = VMESSOutboundConfigurationObject(
                    vnext = listOf(
                        ServerObject(
                            address = address,
                            port = json.get("port").asInt,
                            users = listOf(
                                UserObject(
                                    id = uuid,
                                    level = 8,
                                    security = json.get("scy")?.asString?:"auto"
                                )
                            )
                        )
                    )
                ),
                streamSettings = StreamSettingsObject(
                    network = network,
                    security = if (tls == "tls") "tls" else "",
                    rawSettings = if (network == "tcp") RawSettings(
                        header = HttpHeaderObject(
                            request = HttpRequestObject(),
                            type = "http"
                        ),
                    ) else null,
                    kcpSettings = if (network == "kcp") KcpSettings(
                        header = KcpHeaderObject(
                            type = json.get("type")?.asString ?: "none",
                        ),
                        seed = json.get("path")?.asString ?: ""
                    ) else null,
                    tlsSettings = if (tls == "tls") TlsSettings(
                        serverName = if (host.isNotEmpty()) host else address,
                        allowInsecure = false
                    ) else null,
                    grpcSettings = if (network == "grpc") GrpcSettings(
                        serviceName = json.get("path")?.asString ?: ""
                    ) else null,
                    wsSettings = if (network == "ws") WsSettings(
                        path = json.get("path")?.asString ?: "/${uuid}",
                        headers = mapOf(Pair("host", if (host.isNotEmpty()) host else address))
                    ) else null
                ),
                tag = "proxy"
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override suspend fun preParse(link: Link): Node {
        val vmess = decodeVMESS(link.content)
        val json = vmess.others
        return Node(
            id = link.id,
            url = link.content,
            protocolPrefix = link.protocolPrefix,
            subscriptionId = link.subscriptionId,
            address = json.get("add").asString,
            port = json.get("port").asInt,
            selected = link.selected,
            remark = json.get("ps")?.asString ?: "vmess-${json.get("add").asString}-${json.get("port").asInt}",
            countryISO = if (settingsRepo.settingsFlow.first().geoLiteInstall) {
                Device.getCountryISOFromIp(
                    geoPath = "${XrayAppCompatFactory.xrayPATH}/$GEO_LITE",
                    ip = json.get("add").asString
                )
            } else ""
        )
    }
}
