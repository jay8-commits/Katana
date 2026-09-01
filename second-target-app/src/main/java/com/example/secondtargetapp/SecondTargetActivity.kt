package com.example.secondtargetapp

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Independent Target Application #2 (Package: com.example.secondtargetapp)
 * Demonstrates cross-package identity interception.
 */
class SecondTargetActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SecondTargetApp"
    }

    private lateinit var tvSecondTargetAndroidId: TextView
    private lateinit var tvSecondTargetModel: TextView
    private lateinit var tvSecondTargetFingerprint: TextView
    private lateinit var tvSecondTargetTelephony: TextView
    private lateinit var btnSecondRefresh: Button

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second_target)

        tvSecondTargetAndroidId = findViewById(R.id.tvSecondTargetAndroidId)
        tvSecondTargetModel = findViewById(R.id.tvSecondTargetModel)
        tvSecondTargetFingerprint = findViewById(R.id.tvSecondTargetFingerprint)
        tvSecondTargetTelephony = findViewById(R.id.tvSecondTargetTelephony)
        btnSecondRefresh = findViewById(R.id.btnSecondRefresh)

        readSecondTargetData()

        btnSecondRefresh.setOnClickListener {
            readSecondTargetData()
        }
    }

    @SuppressLint("HardwareIds")
    private fun readSecondTargetData() {
        val androidId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN"

        val model = Build.MODEL
        val brand = Build.BRAND
        val fingerprint = Build.FINGERPRINT

        val telephonyResult = try {
            val tm = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager
            if (tm != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                tm.deviceId ?: tm.imei ?: "NO_DEVICE_ID"
            } else {
                "RESTRICTED (Android 10+ requires privileged carrier/system permissions)"
            }
        } catch (e: Exception) {
            "PERMISSION_RESTRICTED (${e.javaClass.simpleName})"
        }

        Log.d(TAG, "SecondTargetApp (com.example.secondtargetapp) read ANDROID_ID: $androidId, Model: $model ($brand)")

        tvSecondTargetAndroidId.text = "Target #2 Read ANDROID_ID:\n$androidId"
        tvSecondTargetModel.text = "Model & Brand: $model ($brand)"
        tvSecondTargetFingerprint.text = "Fingerprint: $fingerprint"
        tvSecondTargetTelephony.text = "Telephony: $telephonyResult"
    }
}
