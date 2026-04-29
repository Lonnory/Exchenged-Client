package com.exchenged.client.model.stream
import com.exchenged.client.model.Sockopt

data class StreamSettingsObject(
    val network: String = "raw",
    val security: String = "none",
    val tlsSettings: TlsSettings? = null,
    val realitySettings: RealitySettings? = null,
    val tcpSettings: RawSettings? = null, // Added this
    val rawSettings: RawSettings? = null,
    val xhttpSettings: XHttpSettings? = null,
    val kcpSettings: KcpSettings? = null,
    val grpcSettings: GrpcSettings? = null,
    val wsSettings: WsSettings? = null,
    val httpUpgradeSettings: HttpUpgradeSettings? = null,
    val hysteriaSettings: HysteriaSettings? = null,
    val finalMask:FinalMask? =null,
    val sockopt: Sockopt? = null,

    @Deprecated("QUIC has been removed in Xray v24.9.7")
    val quicSettings: Any? = null,
    @Deprecated("DomainSocket has been removed in Xray v24.9.7")
    val dsSettings: Any? = null
)

data class XHttpSettings(
    val mode: String? = "splitHttp", // "splitHttp" | "packetStreaming"
    val host: String? = null,
    val path: String? = null,
    val extra: Map<String, String>? = null,
    val scMaxEachPostBytes: String? = null,
    val scMaxConcurrentPosts: String? = null,
    val scMinPostsIntervalMs: String? = null,
    val xmux: Map<String, Any>? = null // v24.9.30 新增
)

data class HttpUpgradeSettings(
    val acceptProxyProtocol: Boolean = false,
    val path: String = "/",
    val host: String = "",
    val headers: Map<String, String>? = null
)

data class HysteriaSettings(
    val version: Int = 2,
    val auth: String? = null,
    val up: String? = null,
    val down: String? = null,
    val congestion: String? = null,
    val obfs: ObfsConfig? = null,
    val udphop: String? = null,
    val udpIdleTimeout: Int? = null,
    val masquerade: MasqueradeConfig? = null
)

data class ObfsConfig(
    val type: String? = null,
    val password: String? = null
)

data class MasqueradeConfig(
    val type: String? = null,
    val dir: String? = null,
    val url: String? = null,
    val rewriteHost: Boolean? = null,
    val insecure: Boolean? = null,
    val content: String? = null,
    val headers: Map<String, String>? = null,
    val statusCode: Int? = null
)

data class FinalMask(
    val tcp: List<Any>? = null,
    val udp: List<Any>? = null,
    val quicParams: Any? = null
)
