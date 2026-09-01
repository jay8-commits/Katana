package com.example.deviceidlab

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.deviceidlab.generator.RandomIdGenerator
import com.example.deviceidlab.manager.DeviceIdentityManager

class MainActivity : AppCompatActivity() {

    private lateinit var identityManager: DeviceIdentityManager
    private lateinit var tvAndroidId: TextView
    private lateinit var tvImei: TextView
    private lateinit var tvSerial: TextView
    private lateinit var tvModel: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnGenerate: Button
    private lateinit var btnApply: Button
    private lateinit var btnVerifyId: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        identityManager = DeviceIdentityManager(this)

        tvAndroidId = findViewById(R.id.tvAndroidId)
        tvImei = findViewById(R.id.tvImei)
        tvSerial = findViewById(R.id.tvSerial)
        tvModel = findViewById(R.id.tvModel)
        tvStatus = findViewById(R.id.tvStatus)
        btnGenerate = findViewById(R.id.btnGenerate)
        btnApply = findViewById(R.id.btnApply)
        btnVerifyId = findViewById(R.id.btnVerifyId)

        renderCurrentIdentity()

        btnGenerate.setOnClickListener {
            val newProfile = RandomIdGenerator.generateProfile("Profile ${System.currentTimeMillis() % 1000}")
            identityManager.saveActiveProfile(newProfile)
            renderCurrentIdentity()
            tvStatus.text = "Generated new identity: ${newProfile.name}"
        }

        btnApply.setOnClickListener {
            renderCurrentIdentity()
            tvStatus.text = "Profile active. Target apps will read this identity via IPC."
        }

        btnVerifyId.setOnClickListener {
            val current = DeviceIdReader.readCurrentIdentity(this)
            tvStatus.text = "Current Host OS Identity Read:\nAndroid ID: ${current.androidId}\nModel: ${current.model} (${current.manufacturer})\nNote: Injected target processes read spoofed ID via NPatch hook."
        }
    }

    private fun renderCurrentIdentity() {
        val profile = identityManager.getActiveProfile()
        tvAndroidId.text = "Android ID: ${profile.androidId}"
        tvImei.text = "IMEI: ${profile.imei}"
        tvSerial.text = "Serial: ${profile.serialNumber}"
        tvModel.text = "Model: ${profile.buildModel} (${profile.buildManufacturer})"
    }
}
