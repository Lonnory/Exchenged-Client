package com.exchenged.client.parser

import com.exchenged.client.XrayAppCompatFactory
import com.exchenged.client.common.GEO_LITE
import com.exchenged.client.common.repository.SettingsRepository
import com.exchenged.client.dto.Link
import com.exchenged.client.model.MuxObject
import com.exchenged.client.dto.Node
import com.exchenged.client.dto.VLESSConfig
import com.exchenged.client.model.OutboundObject
import com.exchenged.client.model.ServerObject
import com.exchenged.client.model.UserObject
import com.exchenged.client.model.VLESSOutboundConfigurationObject
import com.exchenged.client.model.stream.GrpcSettings
import com.exchenged.client.model.stream.RealitySettings
import com.exchenged.client.model.stream.RawSettings
import com.exchenged.client.model.stream.StreamSettingsObject
import com.exchenged.client.model.stream.TlsSettings
import com.exchenged.client.model.stream.WsSettings
import com.exchenged.client.model.stream.XHttpSettings
import com.exchenged.client.utils.Device
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VLESSConfigParser
@Inject constructor(
    override val settingsRepo: SettingsRepository
): AbstractConfigParser<VLESSOutboundConfigurationObject, VLESSConfig>(){
    override fun decodeProtocol(url: String): VLESSConfig {
        val decode = URLDecoder.decode(url, "UTF-8")
        val withoutProtocol = decode.removePrefix("vless://")
        val (mainPart, remark) = withoutProtocol.split("#").let {
            it[0] to if (it.size > 1) it[1] else ""
        }
        val (userAndServer, query) = mainPart.split("?").let {
            it[0] to if (it.size > 1) it[1] else ""
        }
        val (uuid, serverAndPort) = userAndServer.split("@")
        val (server, portStr) = serverAndPort.split(":")
        val port = portStr.toIntOrNull() ?: 0
        val queryParams = query.split("&").mapNotNull {
            val kv = it.split("=")
            if (kv.size == 2) kv[0] to kv[1] else null
        }.toMap()

        return VLESSConfig(
            remark = remark,
            uuid = uuid,
            server = server,
            port = port,
            param = queryParams
        )
    }

    override fun encodeProtocol(protocol: VLESSConfig): String {
        val mainPart = "${protocol.uuid}@${protocol.server}:${protocol.port}"
        val query = protocol.param.entries.joinToString("&") { "${it.key}=${it.value}" }
        val remarkEncoded = protocol.remark?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        return buildString {
            append("vless://")
            append(mainPart)
            if (query.isNotEmpty()) {
                append("?")
                append(query)
            }
            if (remarkEncoded.isNotEmpty()) {
                append("#")
                append(remarkEncoded)
            }
        }
    }

    companion object {
        const val TAG = "VLESSConfigParser"
    }

    override fun parseOutbound(url: String): OutboundObject<VLESSOutboundConfigurationObject> {
        val parseVLESS = decodeProtocol(url)
        val queryParams = parseVLESS.param
        val network = queryParams["type"]?.let { if (it == "raw") "tcp" else it } ?: "tcp"
        val security = queryParams["security"] ?: "none"
        return OutboundObject(
            protocol = "vless",
            settings = VLESSOutboundConfigurationObject(
                vnext = listOf(
                    ServerObject(
                        address = parseVLESS.server,
                        port = parseVLESS.port,
                        users = listOf(
                            UserObject(
                                id = parseVLESS.uuid,
                                encryption = if (security == "reality") "none" else (queryParams["encryption"] ?: "none"),
                                flow = queryParams["flow"]?.ifBlank { if (security == "reality") "xtls-rprx-vision" else "" } ?: if (security == "reality") "xtls-rprx-vision" else "",
                                level = 0
                            )
                        )
                    )
                )
            ),
            streamSettings = StreamSettingsObject(
                network = network,
                security = security,
                realitySettings = if (security == "reality") {
                    val sni = queryParams["sni"]?.ifBlank { queryParams["host"] }?.ifBlank { parseVLESS.server } ?: queryParams["host"] ?: parseVLESS.server
                    RealitySettings(
                        fingerprint = queryParams["fp"]?.ifBlank { "chrome" } ?: "chrome",
                        publicKey = queryParams["pbk"] ?: "",
                        serverName = sni,
                        serverNames = listOf(sni),
                        shortIds = queryParams["sid"]?.let { if (it.isBlank()) emptyList() else listOf(it) } ?: emptyList(),
                        spiderX = queryParams["spx"] ?: "",
                        show = false,
                    )
                } else null,
                tcpSettings = if (network == "tcp") RawSettings() else null,
                wsSettings = if (network == "ws") {
                    WsSettings(
                        path = queryParams["path"]?.ifBlank { "/" } ?: "/",
                        headers = mapOf(Pair("host", queryParams["host"]?.ifBlank { parseVLESS.server } ?: parseVLESS.server))
                    )
                } else null,
                grpcSettings = if (network == "grpc") GrpcSettings(
                    serviceName = queryParams["serviceName"]?:"",
                    multiMode = false
                ) else null,
                tlsSettings = if (security == "tls") {
                    TlsSettings(serverName = queryParams["sni"]?.ifBlank { queryParams["host"] }?.ifBlank { parseVLESS.server } ?: queryParams["host"] ?: parseVLESS.server )
                } else null,
                xhttpSettings = if (network == "xhttp") {
                    XHttpSettings(
                        mode = queryParams["mode"],
                        host = queryParams["host"],
                        path = queryParams["path"],
                        extra = null // todo
                    )
                } else null
            ),
            mux = MuxObject(concurrency = -1, enable = false, xudpConcurrency = 8, xudpProxyUDP443 = ""),
            tag = "proxy"
        )
    }

    override suspend fun preParse(link: Link): Node {
        val vlessConfig = decodeProtocol(link.content)
        return Node(
            id = link.id,
            url = link.content,
            protocolPrefix = link.protocolPrefix,
            subscriptionId = link.subscriptionId,
            address = vlessConfig.server,
            port = vlessConfig.port,
            selected = link.selected,
            remark = vlessConfig.remark,
            countryISO = if (settingsRepo.settingsFlow.first().geoLiteInstall) {
                Device.getCountryISOFromIp(
                    geoPath = "${XrayAppCompatFactory.xrayPATH}/$GEO_LITE",
                    ip = vlessConfig.server
                )
            } else ""
        )
    }
}
