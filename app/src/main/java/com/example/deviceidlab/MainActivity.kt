package com.example.deviceidlab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deviceidlab.database.AppDatabase
import com.example.deviceidlab.demo.DeviceIdHookDemo
import com.example.deviceidlab.demo.HookInvocationLog
import com.example.deviceidlab.demo.InterceptionBridge
import com.example.deviceidlab.model.DeviceIdentity
import com.example.ui.theme.CodeBg
import com.example.ui.theme.CodeText
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TagAmber
import com.example.ui.theme.TagGreen
import com.example.ui.theme.TagRed
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var identityManager: DeviceIdentityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        identityManager = DeviceIdentityManager(db.usedIdentityDao())

        setContent {
            MyApplicationTheme {
                DeviceLabApp(identityManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLabApp(identityManager: DeviceIdentityManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State flows
    val currentIdentity by identityManager.currentIdentity.collectAsState()
    val usedCount by identityManager.usedCount.collectAsState()
    val isInterceptionActive by InterceptionBridge.isInterceptionActive.collectAsState()
    val invocationLogs by InterceptionBridge.invocationLogs.collectAsState()

    // Real device identifiers state
    var realAndroidIdResult by remember { mutableStateOf(DeviceIdReader.readAndroidId(context)) }
    var realTelephonyResult by remember { mutableStateOf(DeviceIdReader.readTelephonyDeviceId(context)) }

    // Dialog & error states
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var poolExhaustedMessage by remember { mutableStateOf<String?>(null) }
    var lastSimulatedCallOutput by remember { mutableStateOf<String?>(null) }

    // Initial load
    LaunchedEffect(Unit) {
        identityManager.initialize()
    }

    // Keep demo bridge synchronized with active simulated identity
    LaunchedEffect(currentIdentity) {
        InterceptionBridge.updateActiveSimulatedIds(
            androidTestId = currentIdentity?.androidTestId,
            telephonyTestId = currentIdentity?.telephonyTestId
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Package: com.example.deviceidlab",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Educational Lab Overview Banner
            item {
                EducationalBanner()
            }

            // Pool Exhausted Alert if applicable
            if (poolExhaustedMessage != null) {
                item {
                    PoolExhaustedAlert(
                        message = poolExhaustedMessage!!,
                        onDismiss = { poolExhaustedMessage = null }
                    )
                }
            }

            // Section 1: Real Device Identifiers
            item {
                RealDeviceIdentifiersCard(
                    androidIdResult = realAndroidIdResult,
                    telephonyResult = realTelephonyResult,
                    onRefresh = {
                        realAndroidIdResult = DeviceIdReader.readAndroidId(context)
                        realTelephonyResult = DeviceIdReader.readTelephonyDeviceId(context)
                        scope.launch {
                            snackbarHostState.showSnackbar("Real device identifiers refreshed")
                        }
                    }
                )
            }

            // Section 2: Simulated Identity (1,000,000 Pool)
            item {
                SimulatedIdentityCard(
                    currentIdentity = currentIdentity,
                    usedCount = usedCount,
                    totalPool = identityManager.totalPoolCapacity,
                    onGenerate = {
                        scope.launch {
                            when (val result = identityManager.generateNextIdentity()) {
                                is GenerationResult.Success -> {
                                    poolExhaustedMessage = null
                                    snackbarHostState.showSnackbar("Allocated ID #${result.identity.identityNumber}")
                                }
                                is GenerationResult.PoolExhausted -> {
                                    poolExhaustedMessage = result.message
                                }
                                is GenerationResult.Error -> {
                                    snackbarHostState.showSnackbar("Error: ${result.message}")
                                }
                            }
                        }
                    },
                    onReset = { showResetConfirmDialog = true }
                )
            }

            // Section 3: NPatch / Interception Layer Demonstration
            item {
                InterceptionDemoCard(
                    isInterceptionActive = isInterceptionActive,
                    currentIdentity = currentIdentity,
                    lastOutput = lastSimulatedCallOutput,
                    onToggleActive = { InterceptionBridge.setInterceptionActive(it) },
                    onTestAndroidIdHook = {
                        val result = DeviceIdHookDemo.interceptSettingsSecureGetString(
                            callerPackage = context.packageName,
                            settingName = "android_id",
                            originalProvider = { realAndroidIdResult.value }
                        )
                        lastSimulatedCallOutput = "Settings.Secure.getString(ANDROID_ID) => $result"
                    },
                    onTestTelephonyHook = {
                        val result = DeviceIdHookDemo.interceptTelephonyGetDeviceId(
                            callerPackage = context.packageName,
                            originalProvider = { realTelephonyResult.value }
                        )
                        lastSimulatedCallOutput = "TelephonyManager.getDeviceId() => $result"
                    }
                )
            }

            // Section 4: Live Interception Invocations Log
            if (invocationLogs.isNotEmpty()) {
                item {
                    Text(
                        text = "INTERCEPTION INVOCATION LOGS (${invocationLogs.size})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(invocationLogs) { log ->
                    HookLogItem(log = log)
                }

                item {
                    OutlinedButton(
                        onClick = { InterceptionBridge.clearLogs() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("clear_logs_button")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CLEAR INVOCATION LOGS")
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Confirmation Dialog for Database Reset
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = TagAmber) },
            title = { Text(text = stringResource(R.string.dialog_reset_title)) },
            text = { Text(text = stringResource(R.string.dialog_reset_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmDialog = false
                        scope.launch {
                            identityManager.resetDatabase()
                            InterceptionBridge.clearLogs()
                            poolExhaustedMessage = null
                            lastSimulatedCallOutput = null
                            snackbarHostState.showSnackbar("Test database reset successfully (0/1,000,000 used)")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("dialog_confirm_reset_button")
                ) {
                    Text(stringResource(R.string.dialog_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetConfirmDialog = false },
                    modifier = Modifier.testTag("dialog_cancel_reset_button")
                ) {
                    Text(stringResource(R.string.dialog_reset_cancel))
                }
            }
        )
    }
}

@Composable
fun EducationalBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Educational Laboratory demonstrating how Android APIs read device identifiers, platform security restrictions, deterministic 1M identity pool allocation, and controlled method interception.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun RealDeviceIdentifiersCard(
    androidIdResult: RealIdResult,
    telephonyResult: RealIdResult,
    onRefresh: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.section_real_identifiers),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_real_values_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Real Values",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Android ID field
            IdDisplayField(
                label = stringResource(R.string.label_android_id),
                value = androidIdResult.value,
                isRestricted = androidIdResult.isRestricted,
                statusDetail = androidIdResult.statusDetail
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Telephony Device ID field
            IdDisplayField(
                label = stringResource(R.string.label_telephony_id),
                value = telephonyResult.value,
                isRestricted = telephonyResult.isRestricted,
                statusDetail = telephonyResult.statusDetail
            )
        }
    }
}

@Composable
fun SimulatedIdentityCard(
    currentIdentity: DeviceIdentity?,
    usedCount: Int,
    totalPool: Long,
    onGenerate: () -> Unit,
    onReset: () -> Unit
) {
    val progress = (usedCount.toFloat() / totalPool.toFloat()).coerceIn(0f, 1f)
    val percentFormatted = String.format(Locale.US, "%.4f%%", progress * 100)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.section_simulated_identity),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                // Tag badge
                Surface(
                    color = TagGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_simulated_test_value),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TagGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Identity details
            if (currentIdentity != null) {
                IdDisplayField(
                    label = stringResource(R.string.label_identity_number),
                    value = "#${currentIdentity.identityNumber} / 1,000,000",
                    isCode = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                IdDisplayField(
                    label = stringResource(R.string.label_android_test_id),
                    value = currentIdentity.androidTestId,
                    isCode = true,
                    statusDetail = "Deterministic 16-hex hash from index #${currentIdentity.identityNumber}"
                )

                Spacer(modifier = Modifier.height(10.dp))

                IdDisplayField(
                    label = stringResource(R.string.label_telephony_test_id),
                    value = currentIdentity.telephonyTestId,
                    isCode = true,
                    statusDetail = "Deterministic 15-digit simulated IMEI from index #${currentIdentity.identityNumber}"
                )

                Spacer(modifier = Modifier.height(6.dp))

                val formattedDate = remember(currentIdentity.createdAt) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(currentIdentity.createdAt))
                }
                Text(
                    text = "Allocated: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Test Identity Generated Yet",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Tap 'GENERATE NEW ID' to draw a unique unused identity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pool statistics bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "1M Pool Used: $usedCount / 1,000,000",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = percentFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onGenerate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_new_id_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_generate_new), fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.testTag("reset_test_database_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.btn_reset_database), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun InterceptionDemoCard(
    isInterceptionActive: Boolean,
    currentIdentity: DeviceIdentity?,
    lastOutput: String?,
    onToggleActive: (Boolean) -> Unit,
    onTestAndroidIdHook: () -> Unit,
    onTestTelephonyHook: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.section_interception_demo),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Scoped strictly to com.example.deviceidlab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isInterceptionActive,
                    onCheckedChange = onToggleActive,
                    modifier = Modifier.testTag("toggle_interception_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Simulates how an instrumentation module intercepts getIdentifier calls from this test APK and returns the active test ID.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onTestAndroidIdHook,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_android_id_hook_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call Android ID", fontSize = 11.sp)
                }

                FilledTonalButton(
                    onClick = onTestTelephonyHook,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_telephony_hook_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Call Telephony ID", fontSize = 11.sp)
                }
            }

            if (lastOutput != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CodeBg)
                        .padding(10.dp)
                ) {
                    Text(
                        text = lastOutput,
                        fontFamily = FontFamily.Monospace,
                        color = CodeText,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun IdDisplayField(
    label: String,
    value: String,
    isRestricted: Boolean = false,
    isCode: Boolean = false,
    statusDetail: String = ""
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCode) CodeBg else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = value,
                style = if (isCode) MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCode) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isCode -> CodeText
                    isRestricted -> TagAmber
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(label, value)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = if (isCode) CodeText.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (statusDetail.isNotBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusDetail,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = if (isRestricted) TagAmber else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HookLogItem(log: HookInvocationLog) {
    val formattedTime = remember(log.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.targetApi,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    color = if (log.wasIntercepted) TagGreen.copy(alpha = 0.15f) else TagAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (log.wasIntercepted) "SUBSTITUTED" else "PASSTHROUGH",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (log.wasIntercepted) TagGreen else TagAmber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Caller: ${log.callerPackage} | Time: $formattedTime",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Returned: ${log.returnedValue}",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = if (log.wasIntercepted) TagGreen else MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun PoolExhaustedAlert(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TagRed.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = TagRed)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pool_exhausted_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TagRed
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
