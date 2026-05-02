package com.exchenged.client.parser

import com.exchenged.client.common.repository.SettingsRepository
import com.exchenged.client.model.AbsOutboundConfigurationObject
import com.exchenged.client.model.ApiObject
import com.exchenged.client.model.DnsObject
import com.exchenged.client.model.InboundObject
import com.exchenged.client.dto.Link
import com.exchenged.client.model.LogObject
import com.exchenged.client.dto.Node
import com.exchenged.client.model.NoneOutboundConfigurationObject
import com.exchenged.client.model.OutboundObject
import com.exchenged.client.model.PolicyObject
import com.exchenged.client.model.RoutingObject
import com.exchenged.client.model.RuleObject
import com.exchenged.client.model.SniffingObject
import com.exchenged.client.model.SocksInboundConfigurationObject
import com.exchenged.client.model.SystemPolicyObject
import com.exchenged.client.model.TunInboundConfigurationObject
import com.exchenged.client.model.TunnelInboundConfigurationObject
import com.exchenged.client.model.XrayConfiguration
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

/**
 *
 * An abstract parser that provides parsing of common structures.
 * The specific content of each protocol is implemented by its subclass parser.
 * This parser defines the parsing standard for JSON configuration files.
 *
 */

abstract class AbstractConfigParser<T: AbsOutboundConfigurationObject,P> {

    private var apiEnable: Boolean = false

    abstract val settingsRepo: SettingsRepository


    abstract fun decodeProtocol(url: String): P

    abstract fun encodeProtocol(protocol: P): String
    suspend fun getBaseInboundConfig(): InboundObject {
        val settingsState = settingsRepo.settingsFlow.first()
        val auth = if (settingsState.socksUserName.isNotBlank() && settingsState.socksPassword.isNotBlank()) "password" else "noauth"
        return InboundObject(
            listen = settingsState.socksListen.ifBlank { "127.0.0.1" },
            port = settingsState.socksPort,
            protocol = "socks",
            settings = SocksInboundConfigurationObject(
                auth = auth,
                accounts = if (auth == "password") listOf(SocksInboundConfigurationObject.AccountObject(
                    user = settingsState.socksUserName,
                    pass = settingsState.socksPassword
                )) else null,
                udp = true,
                userLevel = 8
            ),
            sniffing = SniffingObject(
                destOverride = listOf("http","tls"),
                enabled = true
            ),
            tag = "socks"
        )
    }

    suspend fun getTunInboundConfig(): InboundObject {
        return InboundObject(
            port = 0,
            protocol = "tun",
            settings = TunInboundConfigurationObject(
                name ="xray0",
                MTU =1400,
                userLevel = 8
            ),
            sniffing = SniffingObject(
                destOverride = listOf("http","tls", "fakedns"),
                enabled = true,
                routeOnly = true
            ),
            tag = "tun"
        )
    }

    fun getAPIInboundConfig(): InboundObject {
        return InboundObject(
            listen = "127.0.0.1",
            port = 10085,
            protocol = "dokodemo-door",
            settings = TunnelInboundConfigurationObject(
                address = "127.0.0.1"
            ),
            tag = "api"
        )
    }

    fun getBaseOutboundConfig(): OutboundObject<NoneOutboundConfigurationObject> {

        return OutboundObject(
            protocol = "freedom",
            tag = "direct",
            settings = NoneOutboundConfigurationObject()
        )
    }

    fun getBaseLogObject(forPing: Boolean = false): LogObject {
        return LogObject(
            logLevel = if (forPing) "none" else "warning"
        )
    }


    suspend fun getBaseDnsConfig(forPing: Boolean = false): DnsObject {
        val settingsState = settingsRepo.settingsFlow.first()
        val dnsV4 = settingsState.dnsIPv4.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        var dns: List<String>
        if (settingsState.ipV6Enable) {
            val dnsV6 = settingsState.dnsIPv6.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            dns = dnsV4 + dnsV6
        }else {
            dns = dnsV4
        }
        if (dns.isEmpty()) {
            dns = listOf("1.1.1.1", "8.8.8.8", "2001:4860:4860::8888")
        }
        return DnsObject(
            hosts = mapOf(
                "domain:googleapis.cn" to "googleapis.com"
            ),
            servers = if (forPing) dns else listOf("fakedns") + dns
        )
    }


    fun getBaseRoutingObject(forPing: Boolean = false): RoutingObject {

        return RoutingObject(
                domainStrategy = if (forPing) "AsIs" else "IPIfNonMatch",
                rules = if (forPing) {
                    listOf(
                        RuleObject(
                            outboundTag = "dns-out",
                            port = "53"
                        ),
                        RuleObject(
                            outboundTag = "proxy",
                            network = "tcp,udp"
                        )
                    )
                } else {
                    listOf(
                        RuleObject(
                            inboundTag = listOf("tun", "socks"),
                            outboundTag = "dns-out",
                            port = "53"
                        ),
                        RuleObject(
                            type = "field",
                            outboundTag = "proxy",
                            domain = listOf("geosite:telegram"),
                        ),
                        RuleObject(
                            type = "field",
                            outboundTag = "proxy",
                            ip = listOf("geoip:telegram")
                        ),
                        RuleObject(
                            type = "field",
                            outboundTag = "proxy",
                            domain = listOf("geosite:geolocation-!cn")
                        ),
                        RuleObject(
                            type = "field",
                            outboundTag = "direct",
                            domain = listOf("geosite:geolocation-cn")
                        ),
                        RuleObject(
                            inboundTag = listOf("api"),
                            outboundTag = "api",
                            type = "field"
                        )
                    )
                }
        )
    }

    private fun getBaseAPIObject(): ApiObject {
        apiEnable = true
        return ApiObject(
            tag = "api",
            services = listOf(
                "StatsService"
            )
        )
    }

    private fun getBasePolicyObject(): PolicyObject {
        return PolicyObject(
            system = SystemPolicyObject(
                statsOutboundUplink = true,
                statsOutboundDownlink = true,
                statsInboundUplink = true,
                statsInboundDownlink = true
            )
        )
    }

    suspend fun parse(link: String, includeTun: Boolean = false, forPing: Boolean = false):String {

        val inbounds = mutableListOf<InboundObject>()
        
        if (!forPing) {
            inbounds.add(getBaseInboundConfig())
            inbounds.add(getAPIInboundConfig())
            if (includeTun) {
                inbounds.add(getTunInboundConfig())
            }
        } else {
            // For ping, we need at least one inbound for Xray to start, 
            // but it should be on a random port or a port that doesn't conflict.
            // Actually, Xray-core allows starting with no inbounds in some cases, 
            // but to be safe, we'll add a dummy one on port 0 (random).
            inbounds.add(InboundObject(
                port = 0,
                protocol = "dokodemo-door",
                settings = TunnelInboundConfigurationObject(address = "127.0.0.1"),
                tag = "dummy"
            ))
        }

        val vlessConfig = XrayConfiguration(
            stats = emptyMap(), // enable
            api = if (forPing) null else getBaseAPIObject(),
            dns = getBaseDnsConfig(forPing),
            log = getBaseLogObject(forPing),
            policy = getBasePolicyObject(),
            inbounds = inbounds,
            outbounds = listOf(
                parseOutbound(link),
                getBaseOutboundConfig(),
                getDnsOutboundConfig()
            ),
            routing = getBaseRoutingObject(forPing),
        )
        val config = Gson().toJson(vlessConfig)
        println(config)
        return config
    }

    private fun getDnsOutboundConfig(): OutboundObject<NoneOutboundConfigurationObject> {
        return OutboundObject(
            protocol = "dns",
            tag = "dns-out",
            settings = NoneOutboundConfigurationObject()
        )
    }

    @Throws(Exception::class)
    abstract fun parseOutbound(url: String): OutboundObject<T>
    @Throws(Exception::class)
    abstract suspend fun preParse(link: Link): Node
}