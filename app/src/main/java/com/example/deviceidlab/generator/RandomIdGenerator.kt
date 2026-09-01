package com.example.deviceidlab.generator

import com.example.deviceidlab.model.DeviceProfile
import java.security.SecureRandom
import java.util.UUID

object RandomIdGenerator {
    private val random = SecureRandom()
    private val hexChars = "0123456789abcdef".toCharArray()

    fun generateRandomHex(length: Int): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(hexChars[random.nextInt(hexChars.size)])
        }
        return sb.toString()
    }

    fun generateRandomImei(): String {
        val sb = StringBuilder(14)
        for (i in 0 until 14) {
            sb.append(random.nextInt(10))
        }
        val sum = calculateLuhnSum(sb.toString())
        val checkDigit = (10 - (sum % 10)) % 10
        sb.append(checkDigit)
        return sb.toString()
    }

    private fun calculateLuhnSum(digits: String): Int {
        var sum = 0
        var alternate = true
        for (i in digits.length - 1 downTo 0) {
            var n = Character.getNumericValue(digits[i])
            if (alternate) {
                n *= 2
                if (n > 9) n = (n % 10) + 1
            }
            sum += n
            alternate = !alternate
        }
        return sum
    }

    fun generateRandomMac(): String {
        val mac = ByteArray(6)
        random.nextBytes(mac)
        mac[0] = (mac[0].toInt() and 0xFE.toByte().toInt()).toByte()
        mac[0] = (mac[0].toInt() or 0x02).toByte()
        return mac.joinToString(":") { String.format("%02X", it) }
    }

    fun generateProfile(name: String = "Random Profile"): DeviceProfile {
        val models = listOf(
            Triple("Pixel 7", "Google", "google"),
            Triple("Galaxy S23", "Samsung", "samsung"),
            Triple("Xiaomi 13", "Xiaomi", "xiaomi"),
            Triple("OnePlus 11", "OnePlus", "oneplus")
        )
        val selected = models[random.nextInt(models.size)]
        val id = UUID.randomUUID().toString()
        val androidId = generateRandomHex(16)
        val imei = generateRandomImei()
        val serialNumber = generateRandomHex(12).uppercase()
        val mac = generateRandomMac()
        val model = selected.first
        val manufacturer = selected.second
        val brand = selected.third
        val product = model.lowercase().replace(" ", "_")
        val device = product
        val fingerprint = "$brand/$product/$device:13/TQ3A.230901.001/$androidId:user/release-keys"

        return DeviceProfile(
            id = id,
            name = name,
            androidId = androidId,
            imei = imei,
            serialNumber = serialNumber,
            macAddress = mac,
            buildModel = model,
            buildManufacturer = manufacturer,
            buildBrand = brand,
            buildProduct = product,
            buildDevice = device,
            buildFingerprint = fingerprint
        )
    }
}
