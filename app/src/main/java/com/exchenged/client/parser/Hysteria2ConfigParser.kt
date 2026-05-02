package com.exchenged.client.parser

import com.exchenged.client.XrayAppCompatFactory
import com.exchenged.client.common.GEO_LITE
import com.exchenged.client.common.repository.SettingsRepository
import com.exchenged.client.dto.Hysteria2Config
import com.exchenged.client.dto.Link
import com.exchenged.client.dto.Node
import com.exchenged.client.model.Hysteria2OutboundConfigurationObject
import com.exchenged.client.model.OutboundObject
import com.exchenged.client.model.Sockopt
import com.exchenged.client.model.stream.StreamSettingsObject
import com.exchenged.client.model.stream.TlsSettings
import com.exchenged.client.utils.Device
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Hysteria2ConfigParser @Inject constructor(override val settingsRepo: SettingsRepository)
    : AbstractConfigParser<Hysteria2OutboundConfigurationObject, Hysteria2Config>() {

    override fun decodeProtocol(url: String): Hysteria2Config {
        val decode = URLDecoder.decode(url, "UTF-8")
        val withoutProtocol = decode.removePrefix("hysteria2://")
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
        return Hysteria2Config(
            remark = remark,
            address = server,
            port = port,
            auth = uuid,
            param = queryParams
        )

    }

    override fun encodeProtocol(protocol: Hysteria2Config): String {
        val mainPart = "${protocol.auth}@${protocol.address}:${protocol.port}"
        val query = protocol.param.entries.joinToString("&") { "${it.key}=${it.value}" }
        val remarkEncoded = protocol.remark?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
        return buildString {
            append("hysteria2://")
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


    override fun parseOutbound(url: String): OutboundObject<Hysteria2OutboundConfigurationObject> {
        val hysteria2Config = decodeProtocol(url)
        val alpn = hysteria2Config.param["alpn"] ?: "h3"
        val sni = hysteria2Config.param["sni"] ?: hysteria2Config.address
        return OutboundObject(
            protocol = "hysteria2",
            settings = Hysteria2OutboundConfigurationObject(
                server = hysteria2Config.address,
                port = hysteria2Config.port,
                auth = hysteria2Config.auth,
                up = hysteria2Config.param["up"],
                down = hysteria2Config.param["down"],
                obfs = if (hysteria2Config.param["obfs"] != null) {
                    com.exchenged.client.model.Hysteria2ObfsObject(
                        type = hysteria2Config.param["obfs"],
                        password = hysteria2Config.param["obfs-password"]
                    )
                } else null
            ),
            streamSettings = StreamSettingsObject(
                network = "udp",
                security = "tls",
                sockopt = Sockopt(),
                tlsSettings = TlsSettings(
                    allowInsecure = hysteria2Config.param["allowInsecure"] == "1" || hysteria2Config.param["allowInsecure"] == "true",
                    alpn = listOf(alpn),
                    serverName = sni
                )
            ),
            tag = "proxy",
        )
    }

    override suspend fun preParse(link: Link): Node {
        val h2Config = decodeProtocol(link.content)
        return Node(
            id = link.id,
            url = link.content,
            protocolPrefix = link.protocolPrefix,
            subscriptionId = link.subscriptionId,
            address = h2Config.address,
            port = h2Config.port,
            selected = link.selected,
            remark = h2Config.remark,
            countryISO = if (settingsRepo.settingsFlow.first().geoLiteInstall) {
                Device.getCountryISOFromIp(
                    geoPath = "${XrayAppCompatFactory.xrayPATH}/$GEO_LITE",
                    ip = h2Config.address
                )
            } else ""
        )
    }
}