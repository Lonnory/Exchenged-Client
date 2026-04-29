package com.exchenged.client.parser

import com.exchenged.client.XrayAppCompatFactory
import com.exchenged.client.common.GEO_LITE
import com.exchenged.client.common.repository.SettingsRepository
import com.exchenged.client.dto.Link
import com.exchenged.client.dto.Node
import com.exchenged.client.dto.ShadowSocksConfig
import com.exchenged.client.model.OutboundObject
import com.exchenged.client.model.ShadowSocksOutboundConfigurationObject
import com.exchenged.client.model.ShadowSocksServerObject
import com.exchenged.client.model.Sockopt
import com.exchenged.client.model.stream.RawSettings
import com.exchenged.client.model.stream.StreamSettingsObject
import com.exchenged.client.utils.Device
import kotlinx.coroutines.flow.first
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShadowSocksConfigParser
@Inject constructor(
    override val settingsRepo: SettingsRepository
): AbstractConfigParser<ShadowSocksOutboundConfigurationObject, ShadowSocksConfig>() {
    override fun decodeProtocol(url: String): ShadowSocksConfig {
        require(url.startsWith("ss://")) { "Not a valid Shadowsocks URL" }
        val content = url.removePrefix("ss://")

        // 1. Split the fragment (tag)
        val parts = content.split("#", limit = 2)
        var mainPart = parts[0]
        val tag = if (parts.size > 1) java.net.URLDecoder.decode(parts[1], "UTF-8") else null

        // 2. Remove query parameters (e.g., ?plugin=...) to prevent port parsing errors
        val queryParts = mainPart.split("?", limit = 2)
        mainPart = queryParts[0]

        // 3. Remove trailing slash if present (handles cases like server:port/)
        mainPart = mainPart.trimEnd('/')

        val (base64Part, serverPart) = if (mainPart.contains("@")) {
            val lastAtIndex = mainPart.lastIndexOf("@")
            mainPart.substring(0, lastAtIndex) to mainPart.substring(lastAtIndex + 1)
        } else {
            // Handle ss://base64(method:password@server:port)
            val decodedMain = String(Base64.decode(mainPart, Base64.DEFAULT))
            val atIndex = decodedMain.lastIndexOf("@")
            if (atIndex != -1) {
                val userInfo = decodedMain.substring(0, atIndex)
                val serverInfo = decodedMain.substring(atIndex + 1)
                Base64.encodeToString(userInfo.toByteArray(), Base64.NO_WRAP) to serverInfo
            } else {
                throw IllegalArgumentException("Invalid SS URL")
            }
        }

        val decodedUserInfo = String(Base64.decode(base64Part, Base64.DEFAULT))
        val userParts = decodedUserInfo.split(":", limit = 2)
        val method = userParts[0]
        val password = if (userParts.size > 1) userParts[1] else ""

        val serverParts = serverPart.split(":", limit = 2)
        val server = serverParts[0]
        val portStr = if (serverParts.size > 1) serverParts[1] else "8388"

        return ShadowSocksConfig(
            method = method,
            password = password,
            server = server,
            port = portStr.toInt(), // The port is now guaranteed to be pure digits
            tag = tag
        )
    }

    override fun encodeProtocol(protocol: ShadowSocksConfig): String {
        val userInfo = "${protocol.method}:${protocol.password}"
        val base64UserInfo = Base64.encodeToString(userInfo.toByteArray(), Base64.NO_WRAP)
        val mainPart = "$base64UserInfo@${protocol.server}:${protocol.port}"
        val tagPart = if (!protocol.tag.isNullOrEmpty()) "#${java.net.URLEncoder.encode(protocol.tag, "UTF-8")}" else ""
        return "ss://$mainPart$tagPart"
    }

    override fun parseOutbound(url: String): OutboundObject<ShadowSocksOutboundConfigurationObject> {
        val shadowSocksConfig = decodeProtocol(url)
        return OutboundObject(
            tag = "proxy",
            protocol = "shadowsocks",
            settings = ShadowSocksOutboundConfigurationObject(
                servers = listOf(
                    ShadowSocksServerObject(
                        address = shadowSocksConfig.server,
                        method = shadowSocksConfig.method,
                        password = shadowSocksConfig.password,
                        port = shadowSocksConfig.port
                    )
                )
            ),
            streamSettings = StreamSettingsObject(
                network = "tcp",
                tcpSettings = RawSettings(),
                sockopt = Sockopt(mark = 255)
            )
        )
    }

    override suspend fun preParse(link: Link): Node {
        val shadowSocksConfig = decodeProtocol(link.content)
        return Node(
            id = link.id,
            url = link.content,
            protocolPrefix = "ss",
            subscriptionId = link.subscriptionId,
            port = shadowSocksConfig.port,
            address = shadowSocksConfig.server,
            selected = link.selected,
            remark = shadowSocksConfig.tag,
            countryISO = if (settingsRepo.settingsFlow.first().geoLiteInstall) {
                Device.getCountryISOFromIp(
                    geoPath = "${XrayAppCompatFactory.xrayPATH}/$GEO_LITE",
                    ip = shadowSocksConfig.server
                )
            } else ""
        )
    }
}
