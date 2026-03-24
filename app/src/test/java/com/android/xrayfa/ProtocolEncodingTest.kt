package com.android.xrayfa

import com.android.xrayfa.dto.ShadowSocksConfig
import com.android.xrayfa.dto.TrojanConfig
import com.android.xrayfa.dto.VLESSConfig
import com.android.xrayfa.dto.VMESSConfig
import com.android.xrayfa.parser.ShadowSocksConfigParser
import com.android.xrayfa.parser.TrojanConfigParser
import com.android.xrayfa.parser.VLESSConfigParser
import com.android.xrayfa.parser.VMESSConfigParser
import com.google.gson.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import java.net.URLDecoder

class ProtocolEncodingTest {

    @Test
    fun testVLESSSafeEncoding() {
        val config = VLESSConfig(
            remark = "test-vless",
            uuid = "my-uuid",
            server = "1.2.3.4",
            port = 443,
            param = mapOf(
                "type" to "grpc",
                "security" to "reality",
                "pbk" to "my-public-key"
            )
        )
        val url = VLESSConfigParser.encodeVLESS(config)
        
        assertTrue(url.startsWith("vless://"))
        assertTrue(url.contains("my-uuid@1.2.3.4:443"))
        assertTrue(url.contains("type=grpc"))
        assertTrue(url.contains("security=reality"))
        assertTrue(url.contains("pbk=my-public-key"))
        assertTrue(URLDecoder.decode(url, "UTF-8").contains("#test-vless"))
    }

    @Test
    fun testShadowSocksEncoding() {
        val config = ShadowSocksConfig(
            method = "aes-256-gcm",
            password = "mypassword",
            server = "ss-server.com",
            port = 8388,
            tag = "ss-tag"
        )
        val url = ShadowSocksConfigParser.encodeShadowSocks(config)
        
        assertTrue(url.startsWith("ss://"))
        val base64Part = Base64.getEncoder().encodeToString("aes-256-gcm:mypassword".toByteArray())
        assertTrue(url.contains("$base64Part@ss-server.com:8388"))
        assertTrue(URLDecoder.decode(url, "UTF-8").endsWith("#ss-tag"))
    }

    @Test
    fun testVMESSEncoding() {
        val others = JsonObject().apply {
            addProperty("add", "vmess.com")
            addProperty("port", 10086)
            addProperty("ps", "vmess-ps")
        }
        val config = VMESSConfig(
            uuid = "vmess-uuid",
            tls = "tls",
            host = "vmess-host",
            network = "ws",
            address = "vmess.com",
            others = others
        )
        val url = VMESSConfigParser.encodeVMESS(config)
        
        assertTrue(url.startsWith("vmess://"))
        val base64Content = url.removePrefix("vmess://")
        val decoded = String(Base64.getDecoder().decode(base64Content))
        assertTrue(decoded.contains("\"id\":\"vmess-uuid\""))
        assertTrue(decoded.contains("\"net\":\"ws\""))
        assertTrue(decoded.contains("\"add\":\"vmess.com\""))
        assertTrue(decoded.contains("\"ps\":\"vmess-ps\""))
    }

    @Test
    fun testTrojanEncoding() {
        val config = TrojanConfig(
            scheme = "trojan",
            password = "trojan-password",
            host = "trojan.com",
            port = 443,
            params = mapOf("type" to "ws", "security" to "tls"),
            remark = "trojan-remark",
            original = ""
        )
        val url = TrojanConfigParser.encodeTrojan(config)
        
        assertTrue(url.startsWith("trojan://"))
        assertTrue(url.contains("trojan-password@trojan.com:443"))
        assertTrue(url.contains("type=ws"))
        assertTrue(url.contains("security=tls"))
        assertTrue(URLDecoder.decode(url, "UTF-8").contains("#trojan-remark"))
    }
}
