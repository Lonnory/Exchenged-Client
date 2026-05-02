package com.exchenged.client.parser

import com.exchenged.client.XrayAppCompatFactory
import com.exchenged.client.common.GEO_LITE
import com.exchenged.client.common.repository.SettingsRepository
import com.exchenged.client.dto.Link
import com.exchenged.client.dto.Node
import com.exchenged.client.dto.VMESSConfig
import com.exchenged.client.model.OutboundObject
import com.exchenged.client.model.ServerObject
import com.exchenged.client.model.UserObject
import com.exchenged.client.model.VMESSOutboundConfigurationObject
import com.exchenged.client.model.stream.GrpcSettings
import com.exchenged.client.model.stream.HttpHeaderObject
import com.exchenged.client.model.stream.HttpRequestObject
import com.exchenged.client.model.stream.KcpHeaderObject
import com.exchenged.client.model.stream.KcpSettings
import com.exchenged.client.model.stream.RawSettings
import com.exchenged.client.model.stream.StreamSettingsObject
import com.exchenged.client.model.stream.TlsSettings
import com.exchenged.client.model.stream.WsSettings
import com.exchenged.client.utils.Device
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.first
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VMESSConfigParser
@Inject constructor(
    override val settingsRepo: SettingsRepository
): AbstractConfigParser<VMESSOutboundConfigurationObject, VMESSConfig>() {
    override fun decodeProtocol(url: String): VMESSConfig {
        val cleanLink = url.removePrefix("vmess://").trim()
        val decoded = String(Base64.decode(cleanLink, Base64.DEFAULT))
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

    override fun encodeProtocol(protocol: VMESSConfig): String {
        val json = protocol.others.deepCopy()
        json.addProperty("v", "2")
        json.addProperty("id", protocol.uuid)
        json.addProperty("tls", protocol.tls)
        json.addProperty("host", protocol.host)
        json.addProperty("net", protocol.network)
        json.addProperty("add", protocol.address)

        val jsonString = json.toString()
        val encoded = Base64.encodeToString(jsonString.toByteArray(), Base64.NO_WRAP)
        return "vmess://$encoded"
    }

    companion object {
        const val TAG = "VMESSConfigParser"
    }

    override fun parseOutbound(url: String): OutboundObject<VMESSOutboundConfigurationObject> {
        try {
            val vmess = decodeProtocol(url)
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
                    tcpSettings = if (network == "tcp") {
                        val headerType = json.get("type")?.asString ?: "none"
                        if (headerType == "http") {
                            RawSettings(
                                header = HttpHeaderObject(
                                    request = HttpRequestObject(),
                                    type = "http"
                                ),
                            )
                        } else RawSettings()
                    } else null,
                    kcpSettings = if (network == "kcp") KcpSettings(
                        header = KcpHeaderObject(
                            type = json.get("type")?.asString ?: "none",
                        ),
                        seed = json.get("path")?.asString ?: ""
                    ) else null,
                    tlsSettings = if (tls == "tls") TlsSettings(
                        serverName = json.get("sni")?.asString?.ifBlank { host }?.ifBlank { address }
                            ?: host.ifBlank { address },
                        allowInsecure = false
                    ) else null,
                    grpcSettings = if (network == "grpc") GrpcSettings(
                        serviceName = json.get("path")?.asString ?: ""
                    ) else null,
                    wsSettings = if (network == "ws") WsSettings(
                        path = json.get("path")?.asString?.ifBlank { "/" } ?: "/",
                        headers = mapOf(Pair("host", host.ifBlank { address }))
                    ) else null
                ),
                tag = "proxy"
            )
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override suspend fun preParse(link: Link): Node {
        val vmess = decodeProtocol(link.content)
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
