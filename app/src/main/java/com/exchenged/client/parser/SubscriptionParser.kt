package com.exchenged.client.parser

import com.exchenged.client.dto.Subscription
import android.util.Base64

/**
 *
 * Parse subscription link
 */
class SubscriptionParser {

    fun parse(content: String, originalUrl: String, existingId: Int = -1): Pair<Subscription, List<String>> {
        var mark = "New Subscription"
        var updateInterval: Long = 0
        var userInfo: String? = null
        var supportUrl: String? = null
        var webPageUrl: String? = null
        var isLocked = false
        var announce: String? = null
        val configs = mutableListOf<String>()

        var decodedContent = content
        try {
            // Check if whole content is base64 (standard for v2ray subscriptions)
            val trimmed = content.trim()
            if (!trimmed.startsWith("#") && !trimmed.contains("://") && trimmed.length > 10) {
                val decoded = String(Base64.decode(trimmed.replace("\n", "").replace("\r", ""), Base64.DEFAULT))
                if (decoded.contains("://") || decoded.contains("#")) {
                    decodedContent = decoded
                }
            }
        } catch (e: Exception) {
            // Not base64 or decode failed
        }

        val lines = decodedContent.lines()
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("#")) {
                val header = trimmedLine.substring(1).trim()
                when {
                    header.lowercase().startsWith("profile-title") -> {
                        val value = header.substringAfter(":").trim()
                        mark = if (value.startsWith("base64:")) {
                            try {
                                String(Base64.decode(value.substringAfter("base64:").trim(), Base64.DEFAULT))
                            } catch (e: Exception) {
                                value
                            }
                        } else {
                            value
                        }
                    }
                    header.lowercase().startsWith("profile-update-interval") -> {
                        updateInterval = header.substringAfter(":").trim().toLongOrNull() ?: 0
                    }
                    header.lowercase().startsWith("subscription-userinfo") -> {
                        userInfo = header.substringAfter(":").trim()
                    }
                    header.lowercase().startsWith("support-url") -> {
                        supportUrl = header.substringAfter(":").trim()
                    }
                    header.lowercase().startsWith("profile-web-page-url") -> {
                        webPageUrl = header.substringAfter(":").trim()
                    }
                    header.lowercase().startsWith("profile-locked") -> {
                        isLocked = header.substringAfter(":").trim().lowercase() == "true"
                    }
                    header.lowercase().startsWith("announce") -> {
                        announce = header.substringAfter(":").trim()
                    }
                }
            } else if (trimmedLine.isNotEmpty()) {
                // Handle cases where servers might be in a base64 block within the file
                if (!trimmedLine.contains("://") && trimmedLine.length > 30) {
                    try {
                        val decoded = String(Base64.decode(trimmedLine, Base64.DEFAULT))
                        decoded.lines().forEach { 
                            val l = it.trim()
                            if (l.contains("://")) configs.add(l)
                        }
                    } catch (e: Exception) {
                        configs.add(trimmedLine)
                    }
                } else if (trimmedLine.contains("://")) {
                    configs.add(trimmedLine)
                }
            }
        }

        val subscription = Subscription(
            id = if (existingId == -1) 0 else existingId,
            mark = mark,
            url = originalUrl,
            isAutoUpdate = updateInterval > 0,
            updateInterval = updateInterval,
            userInfo = userInfo,
            supportUrl = supportUrl,
            webPageUrl = webPageUrl,
            isLocked = isLocked,
            announce = announce
        )

        return subscription to configs
    }

    fun parseUrl(content: String): List<String> {
        return try {
            val decode = String(Base64.decode(content, Base64.DEFAULT))
            decode.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            content.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }
}
