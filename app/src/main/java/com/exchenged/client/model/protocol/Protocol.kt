package com.exchenged.client.model.protocol

import com.exchenged.client.model.protocol.Protocol.SHADOW_SOCKS
import com.exchenged.client.model.protocol.Protocol.TROJAN
import com.exchenged.client.model.protocol.Protocol.VLESS
import com.exchenged.client.model.protocol.Protocol.VMESS
import com.exchenged.client.model.protocol.Protocol.HYSTERIA2

/**
 * @param protocolType protocol type
 */
enum class Protocol(
    val protocolType: String
) {
    VLESS("vless"),

    VMESS("vmess"),

    SHADOW_SOCKS("ss"),

    TROJAN("trojan"),

    HYSTERIA2("hysteria2");


}
val protocolsPrefix = listOf(
    VLESS.protocolType,
    VMESS.protocolType,
    SHADOW_SOCKS.protocolType,
    TROJAN.protocolType,
    HYSTERIA2.protocolType
)
val protocolPrefixMap = mapOf(
    SHADOW_SOCKS.protocolType to SHADOW_SOCKS,
    VLESS.protocolType to VLESS,
    VMESS.protocolType to VMESS,
    TROJAN.protocolType to TROJAN,
    HYSTERIA2.protocolType to HYSTERIA2
)
