package com.example.deviceidlab.manager

import com.example.deviceidlab.model.LocationProfile
import com.example.deviceidlab.model.WorldwideLocationProfile

/**
 * Deterministic, zero-dependency JSON serializer for LocationProfile and WorldwideLocationProfile.
 */
object LocationJsonSerializer {

    fun escape(s: String?): String {
        if (s == null) return "null"
        val sb = StringBuilder()
        sb.append('"')
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    fun serialize(p: LocationProfile): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"profileId\":").append(escape(p.profileId)).append(",")
        sb.append("\"latitude\":").append(p.latitude).append(",")
        sb.append("\"longitude\":").append(p.longitude).append(",")
        sb.append("\"altitude\":").append(p.altitude).append(",")
        sb.append("\"accuracy\":").append(p.accuracy).append(",")
        sb.append("\"speed\":").append(p.speed).append(",")
        sb.append("\"bearing\":").append(p.bearing).append(",")
        sb.append("\"timestamp\":").append(p.timestamp).append(",")
        sb.append("\"elapsedRealtimeNanos\":").append(p.elapsedRealtimeNanos).append(",")
        sb.append("\"provider\":").append(escape(p.provider))
        sb.append("}")
        return sb.toString()
    }

    fun serializeWorldwide(p: WorldwideLocationProfile): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"profileId\":").append(escape(p.profileId)).append(",")
        sb.append("\"city\":").append(escape(p.city)).append(",")
        sb.append("\"country\":").append(escape(p.country)).append(",")
        sb.append("\"countryCode\":").append(escape(p.countryCode)).append(",")
        sb.append("\"latitude\":").append(p.latitude).append(",")
        sb.append("\"longitude\":").append(p.longitude).append(",")
        sb.append("\"timezone\":").append(escape(p.timezone)).append(",")
        sb.append("\"altitude\":").append(p.altitude).append(",")
        sb.append("\"accuracy\":").append(p.accuracy).append(",")
        sb.append("\"speed\":").append(p.speed).append(",")
        sb.append("\"bearing\":").append(p.bearing).append(",")
        sb.append("\"timestamp\":").append(p.timestamp).append(",")
        sb.append("\"elapsedRealtimeNanos\":").append(p.elapsedRealtimeNanos).append(",")
        sb.append("\"provider\":").append(escape(p.provider)).append(",")
        sb.append("\"syntheticIp\":").append(escape(p.syntheticIp)).append(",")
        sb.append("\"state\":").append(escape(p.state))
        sb.append("}")
        return sb.toString()
    }

    fun parse(json: String): LocationProfile {
        val map = parseFlatJsonObject(json)
        val profileId = map["profileId"] ?: "loc_unknown"
        val latitude = map["latitude"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Missing or invalid latitude in JSON: $json")
        val longitude = map["longitude"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Missing or invalid longitude in JSON: $json")
        val altitude = map["altitude"]?.toDoubleOrNull() ?: 0.0
        val accuracy = map["accuracy"]?.toFloatOrNull() ?: 5.0f
        val speed = map["speed"]?.toFloatOrNull() ?: 0.0f
        val bearing = map["bearing"]?.toFloatOrNull() ?: 0.0f
        val timestamp = map["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
        val elapsedRealtimeNanos = map["elapsedRealtimeNanos"]?.toLongOrNull() ?: System.nanoTime()
        val provider = map["provider"] ?: "gps"

        return LocationProfile(
            profileId = profileId,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            accuracy = accuracy,
            speed = speed,
            bearing = bearing,
            timestamp = timestamp,
            elapsedRealtimeNanos = elapsedRealtimeNanos,
            provider = provider
        )
    }

    fun parseWorldwide(json: String): WorldwideLocationProfile {
        val map = parseFlatJsonObject(json)
        val profileId = map["profileId"] ?: "loc_unknown"
        val city = map["city"] ?: "Tokyo"
        val country = map["country"] ?: "Japan"
        val countryCode = map["countryCode"] ?: "JP"
        val latitude = map["latitude"]?.toDoubleOrNull() ?: 35.6762
        val longitude = map["longitude"]?.toDoubleOrNull() ?: 139.6503
        val timezone = map["timezone"] ?: "Asia/Tokyo"
        val altitude = map["altitude"]?.toDoubleOrNull() ?: 0.0
        val accuracy = map["accuracy"]?.toFloatOrNull() ?: 5.0f
        val speed = map["speed"]?.toFloatOrNull() ?: 0.0f
        val bearing = map["bearing"]?.toFloatOrNull() ?: 0.0f
        val timestamp = map["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
        val elapsedRealtimeNanos = map["elapsedRealtimeNanos"]?.toLongOrNull() ?: System.nanoTime()
        val provider = map["provider"] ?: "gps"
        val syntheticIp = map["syntheticIp"] ?: "203.0.113.42"
        val state = map["state"] ?: "ACTIVE"

        return WorldwideLocationProfile(
            profileId = profileId,
            city = city,
            country = country,
            countryCode = countryCode,
            latitude = latitude,
            longitude = longitude,
            timezone = timezone,
            provider = provider,
            altitude = altitude,
            accuracy = accuracy,
            speed = speed,
            bearing = bearing,
            timestamp = timestamp,
            elapsedRealtimeNanos = elapsedRealtimeNanos,
            syntheticIp = syntheticIp,
            state = state
        )
    }

    fun parseFlatJsonObject(json: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        val trimmed = json.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return result
        val content = trimmed.substring(1, trimmed.length - 1)

        var i = 0
        while (i < content.length) {
            val keyStartQuote = content.indexOf('"', i)
            if (keyStartQuote == -1) break
            val keyEndQuote = findMatchingQuote(content, keyStartQuote + 1)
            if (keyEndQuote == -1) break
            val key = unescape(content.substring(keyStartQuote + 1, keyEndQuote))

            val colonIndex = content.indexOf(':', keyEndQuote + 1)
            if (colonIndex == -1) break

            var valueStart = colonIndex + 1
            while (valueStart < content.length && content[valueStart].isWhitespace()) {
                valueStart++
            }
            if (valueStart >= content.length) break

            val (value, nextI) = if (content[valueStart] == '"') {
                val valueEndQuote = findMatchingQuote(content, valueStart + 1)
                if (valueEndQuote == -1) Pair("", content.length)
                else Pair(unescape(content.substring(valueStart + 1, valueEndQuote)), valueEndQuote + 1)
            } else {
                var valueEnd = valueStart
                while (valueEnd < content.length && content[valueEnd] != ',' && content[valueEnd] != '}') {
                    valueEnd++
                }
                val rawVal = content.substring(valueStart, valueEnd).trim()
                val parsedVal = if (rawVal == "null") "" else rawVal
                Pair(parsedVal, valueEnd)
            }

            result[key] = value
            val nextComma = content.indexOf(',', nextI)
            if (nextComma == -1) break
            i = nextComma + 1
        }
        return result
    }

    private fun findMatchingQuote(s: String, start: Int): Int {
        var j = start
        while (j < s.length) {
            if (s[j] == '\\') {
                j += 2
                continue
            }
            if (s[j] == '"') {
                return j
            }
            j++
        }
        return -1
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    '"' -> { sb.append('"'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    'b' -> { sb.append('\b'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    else -> { sb.append(s[i + 1]); i += 2 }
                }
            } else {
                sb.append(s[i])
                i++
            }
        }
        return sb.toString()
    }
}
