package com.example.deviceidlab.manager

import kotlin.math.abs

/**
 * Deterministic generator and validator for Synthetic Test IPs.
 *
 * Implements RFC 5737 Test Address Ranges:
 * - TEST-NET-3: 203.0.113.0/24 (Default)
 * - TEST-NET-2: 198.51.100.0/24
 * - TEST-NET-1: 192.0.2.0/24
 *
 * CRITICAL ARCHITECTURAL DISTINCTION:
 * - SYNTHETIC TEST IP: An application-level simulation parameter used for verification.
 * - ACTUAL PUBLIC IP: Controlled by network egress/VPN/proxy/carrier/Wi-Fi interface.
 */
object SyntheticIpGenerator {

    private var counter = 42

    fun generateTestNet3(seed: Long? = null): String {
        val s = seed ?: (counter++).toLong()
        val hostPart = (abs(s) % 250) + 2 // 2..251
        return "203.0.113.$hostPart"
    }

    fun generateUniqueTestIp(existingIps: Set<String>, seed: Long? = null): String {
        var s = seed ?: (counter++).toLong()
        for (attempt in 0..250) {
            val hostPart = ((abs(s) + attempt) % 250) + 2
            val ip = "203.0.113.$hostPart"
            if (!existingIps.contains(ip)) {
                return ip
            }
        }
        return "203.0.113.${(abs(s) % 250) + 2}"
    }

    fun isValidIpv4(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull()
            num != null && num in 0..255 && (part.length == 1 || !part.startsWith("0"))
        }
    }

    fun isRfc5737TestIp(ip: String?): Boolean {
        if (!isValidIpv4(ip)) return false
        val clean = ip!!
        return clean.startsWith("203.0.113.") ||
               clean.startsWith("198.51.100.") ||
               clean.startsWith("192.0.2.")
    }
}
