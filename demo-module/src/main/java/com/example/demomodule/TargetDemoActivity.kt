package com.example.demomodule

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Target Test Application #1
 * Verifies Settings.Secure.ANDROID_ID and Build identity readings.
 */
class TargetDemoActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TargetDemo1"
    }

    private lateinit var tvTargetAndroidId: TextView
    private lateinit var tvTargetBuildModel: TextView
    private lateinit var tvTargetSerial: TextView
    private lateinit var btnRefresh: Button

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_target_demo)

        tvTargetAndroidId = findViewById(R.id.tvTargetAndroidId)
        tvTargetBuildModel = findViewById(R.id.tvTargetBuildModel)
        tvTargetSerial = findViewById(R.id.tvTargetSerial)
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

        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER

        val serial = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                Build.SERIAL
            }
        } catch (e: Exception) {
            "RESTRICTED (${e.javaClass.simpleName})"
        }

        Log.d(TAG, "TargetDemo1 read ANDROID_ID via Settings.Secure: $androidId, Model: $model, Serial: $serial")
        tvTargetAndroidId.text = "Target #1 Read ANDROID_ID:\n$androidId"
        tvTargetBuildModel.text = "Build Model: $model ($manufacturer)"
        tvTargetSerial.text = "Serial: $serial"
    }
}
