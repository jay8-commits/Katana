package com.example.demomodule

import android.annotation.SuppressLint
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TargetDemoActivity : AppCompatActivity() {

    private lateinit var tvTargetAndroidId: TextView
    private lateinit var btnRefresh: Button

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target_demo)

        tvTargetAndroidId = findViewById(R.id.tvTargetAndroidId)
        btnRefresh = findViewById(R.id.btnRefresh)

        readIdentifiers()

        btnRefresh.setOnClickListener {
            readIdentifiers()
        }
    }

    @SuppressLint("HardwareIds")
    private fun readIdentifiers() {
        val androidId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"
        tvTargetAndroidId.text = "Target Read ANDROID_ID:\n$androidId"
    }
}
