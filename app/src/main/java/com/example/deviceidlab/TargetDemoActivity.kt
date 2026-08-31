package com.example.deviceidlab

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeviceIdLabTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Independent Target Demo Activity.
 *
 * Requirements:
 * 1. Explicitly performs standard Android API queries for BOTH:
 *    - Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
 *    - TelephonyManager.getDeviceId() / getImei()
 * 2. Displays and logs the returned values directly on screen.
 * 3. Does NOT obtain expected values through any backdoor test bridge or shared database.
 * 4. Allows testing runtime dynamic ID changes (ID #1 -> ID #2) without repatching or reinstalling.
 */
class TargetDemoActivity : ComponentActivity() {

    companion object {
        const val TAG = "TargetDemoApp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DeviceIdLabTheme {
                TargetDemoScreen(
                    onBackPressed = { finish() },
                    queryAndroidId = { performIndependentAndroidIdQuery() },
                    queryTelephonyId = { performIndependentTelephonyIdQuery() }
                )
            }
        }
    }

    @SuppressLint("HardwareIds")
    private fun performIndependentAndroidIdQuery(): String {
        val readValue = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "null"
        Log.i(TAG, "[$TAG] [TARGET READ] Settings.Secure.getString(contentResolver, ANDROID_ID) returned: '$readValue'")
        return readValue
    }

    @Suppress("DEPRECATION")
    @SuppressLint("HardwareIds")
    private fun performIndependentTelephonyIdQuery(): String {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        if (tm == null) {
            val msg = "TelephonyManager service unavailable"
            Log.w(TAG, "[$TAG] [TARGET READ] $msg")
            return msg
        }

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On Android 10+ (API 29+), getImei() is the official telephony identifier API
                try {
                    val imeiVal = tm.imei
                    val res = if (!imeiVal.isNullOrBlank()) imeiVal else (tm.getDeviceId() ?: "null")
                    Log.i(TAG, "[$TAG] [TARGET READ] TelephonyManager.getImei() returned: '$res'")
                    res
                } catch (se: SecurityException) {
                    val msg = "Restricted (SecurityException: READ_PRIVILEGED_PHONE_STATE required on Android 10+)"
                    Log.w(TAG, "[$TAG] [TARGET READ] Telephony read blocked: $msg")
                    msg
                }
            } else {
                @Suppress("DEPRECATION")
                try {
                    val devId = tm.deviceId ?: "null"
                    Log.i(TAG, "[$TAG] [TARGET READ] TelephonyManager.getDeviceId() returned: '$devId'")
                    devId
                } catch (se: SecurityException) {
                    val msg = "Permission Denied (READ_PHONE_STATE required on Android <10)"
                    Log.w(TAG, "[$TAG] [TARGET READ] Telephony read blocked: $msg")
                    msg
                }
            }
        } catch (t: Throwable) {
            val err = "Exception: ${t.javaClass.simpleName} (${t.message})"
            Log.w(TAG, "[$TAG] [TARGET READ] Telephony read error: $err")
            err
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetDemoScreen(
    onBackPressed: () -> Unit,
    queryAndroidId: () -> String,
    queryTelephonyId: () -> String
) {
    var currentAndroidId by remember { mutableStateOf(queryAndroidId()) }
    var currentTelephonyId by remember { mutableStateOf(queryTelephonyId()) }
    var lastQueryTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Target Demo Application",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Independent Hardware & Identity Reader",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackPressed,
                        modifier = Modifier.testTag("target_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Target Info Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Target Application Runtime",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "This independent process invokes official platform APIs (Settings.Secure for Android ID, TelephonyManager for Device/IMEI ID). Under NPatch 1.0.7, calls are intercepted at the framework layer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Live Read Output Card - Android ID
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. ANDROID ID READ OUTPUT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Settings.Secure.getString(..., ANDROID_ID):",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentAndroidId,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("target_android_id_text")
                            )
                        }
                    }
                }
            }

            // Live Read Output Card - Telephony Device ID
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. TELEPHONY DEVICE ID READ OUTPUT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "TelephonyManager.getDeviceId() / getImei():",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentTelephonyId,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF4ADE80),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("target_telephony_id_text")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Last Query: ${timeFormatter.format(Date(lastQueryTimestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "PID: ${android.os.Process.myPid()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Action Buttons
            Button(
                onClick = {
                    currentAndroidId = queryAndroidId()
                    currentTelephonyId = queryTelephonyId()
                    lastQueryTimestamp = System.currentTimeMillis()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("target_refresh_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "RE-QUERY ALL IDENTIFIERS",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onBackPressed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("return_to_lab_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("RETURN TO LAB CONTROLLER")
            }
        }
    }
}

