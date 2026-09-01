package com.example.deviceidlab

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.deviceidlab.generator.RandomIdGenerator
import com.example.deviceidlab.hook.TestApiCatalog
import com.example.deviceidlab.manager.DeviceIdentityManager
import com.example.deviceidlab.model.DeviceProfile

class MainActivity : AppCompatActivity() {

    private lateinit var identityManager: DeviceIdentityManager
    private lateinit var tvAndroidId: TextView
    private lateinit var tvImei: TextView
    private lateinit var tvSerial: TextView
    private lateinit var tvMac: TextView
    private lateinit var tvModel: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCatalogList: TextView
    private lateinit var btnGenerateProfile1: Button
    private lateinit var btnGenerateProfile2: Button
    private lateinit var btnGenerateRandom: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        identityManager = DeviceIdentityManager(this)

        tvAndroidId = findViewById(R.id.tvAndroidId)
        tvImei = findViewById(R.id.tvImei)
        tvSerial = findViewById(R.id.tvSerial)
        tvMac = findViewById(R.id.tvMac)
        tvModel = findViewById(R.id.tvModel)
        tvStatus = findViewById(R.id.tvStatus)
        tvCatalogList = findViewById(R.id.tvCatalogList)

        btnGenerateProfile1 = findViewById(R.id.btnGenerateProfile1)
        btnGenerateProfile2 = findViewById(R.id.btnGenerateProfile2)
        btnGenerateRandom = findViewById(R.id.btnGenerateRandom)

        renderCurrentIdentity()
        renderCatalog()

        btnGenerateProfile1.setOnClickListener {
            val p1 = DeviceProfile(
                id = "profile_pixel_7",
                name = "Google Pixel 7 (Profile #1)",
                androidId = "a1b2c3d4e5f60718",
                imei = "864201041234567",
                serialNumber = "27161FDH200001",
                macAddress = "02:00:00:11:22:33",
                buildModel = "Pixel 7",
                buildManufacturer = "Google",
                buildBrand = "google",
                buildProduct = "panther",
                buildDevice = "panther",
                buildFingerprint = "google/panther/panther:13/TQ3A.230901.001/10750709:user/release-keys"
            )
            identityManager.saveActiveProfile(p1)
            renderCurrentIdentity()
            tvStatus.text = "Active: Profile #1 (Pixel 7). Target apps will observe this upon read/restart."
        }

        btnGenerateProfile2.setOnClickListener {
            val p2 = DeviceProfile(
                id = "profile_s23",
                name = "Samsung Galaxy S23 (Profile #2)",
                androidId = "f9e8d7c6b5a41320",
                imei = "359876543210987",
                serialNumber = "R58M30ABCDE",
                macAddress = "02:00:00:AA:BB:CC",
                buildModel = "SM-S911B",
                buildManufacturer = "samsung",
                buildBrand = "samsung",
                buildProduct = "dm1qxxx",
                buildDevice = "dm1q",
                buildFingerprint = "samsung/dm1qxxx/dm1q:14/UP1A.231005.007/S911BXXU3BWJM:user/release-keys"
            )
            identityManager.saveActiveProfile(p2)
            renderCurrentIdentity()
            tvStatus.text = "Active: Profile #2 (Galaxy S23). Switched dynamically without repatching."
        }

        btnGenerateRandom.setOnClickListener {
            val newProfile = RandomIdGenerator.generateProfile("Random Profile ${System.currentTimeMillis() % 1000}")
            identityManager.saveActiveProfile(newProfile)
            renderCurrentIdentity()
            tvStatus.text = "Active: ${newProfile.name} (Randomized)."
        }
    }

    private fun renderCurrentIdentity() {
        val profile = identityManager.getActiveProfile()
        tvAndroidId.text = "Android ID: ${profile.androidId} [Masked: ${TestApiCatalog.maskValue(profile.androidId)}]"
        tvImei.text = "IMEI: ${profile.imei} [Masked: ${TestApiCatalog.maskValue(profile.imei)}]"
        tvSerial.text = "Serial: ${profile.serialNumber} [Masked: ${TestApiCatalog.maskValue(profile.serialNumber)}]"
        tvMac.text = "MAC: ${profile.macAddress} [Masked: ${TestApiCatalog.maskValue(profile.macAddress)}]"
        tvModel.text = "Model: ${profile.buildModel} (${profile.buildManufacturer}) [Product: ${profile.buildProduct}]"
    }

    private fun renderCatalog() {
        val sb = StringBuilder()
        TestApiCatalog.SUPPORTED_APIS.forEachIndexed { index, api ->
            val mode = if (api.isDynamic) "DYNAMIC" else "STATIC (Restart Required)"
            sb.append("${index + 1}. ${api.name}\n   [$mode] -> Key: ${api.configKey}\n")
        }
        tvCatalogList.text = sb.toString()
    }
}
