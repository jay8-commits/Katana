package com.example.deviceidlab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import com.example.deviceidlab.hook.NPatchHookEntry
import com.example.deviceidlab.model.DeviceIdentity
import com.example.ui.theme.DeviceIdLabTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom tag colors
val TagAmber = Color(0xFFD97706)
val TagGreen = Color(0xFF16A34A)
val TagRed = Color(0xFFDC2626)
val TagBlue = Color(0xFF2563EB)
val CodeBg = Color(0xFF1E293B)
val CodeText = Color(0xFF38BDF8)

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getInstance(applicationContext) }
    private val identityManager by lazy {
        DeviceIdentityManager(
            identityDao = database.usedIdentityDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DeviceIdLabTheme {
                MainScreen(identityManager = identityManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(identityManager: DeviceIdentityManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State from Identity Manager
    val currentIdentity by identityManager.currentIdentity.collectAsState()
    val usedCount by identityManager.usedCount.collectAsState()

    // State from Interception Bridge
    val isInterceptionActive by InterceptionBridge.isInterceptionActive.collectAsState()
    val invocationLogs by InterceptionBridge.invocationLogs.collectAsState()

    // State from DeviceIdProvider (Dynamic Runtime IPC Bridge)
    val providerAndroidTestId by com.example.deviceidlab.provider.DeviceIdProvider.currentAndroidTestIdFlow.collectAsState()
    val providerTelephonyTestId by com.example.deviceidlab.provider.DeviceIdProvider.currentTelephonyTestIdFlow.collectAsState()
    val providerLastInterceptedAndroidId by com.example.deviceidlab.provider.DeviceIdProvider.lastInterceptedAndroidIdFlow.collectAsState()
    val providerLastInterceptedTelephonyId by com.example.deviceidlab.provider.DeviceIdProvider.lastInterceptedTelephonyIdFlow.collectAsState()
    val providerLastTargetReadAndroid by com.example.deviceidlab.provider.DeviceIdProvider.lastTargetReadAndroidIdFlow.collectAsState()
    val providerLastTargetReadTelephony by com.example.deviceidlab.provider.DeviceIdProvider.lastTargetReadTelephonyIdFlow.collectAsState()
    val isTargetProcessDetected by com.example.deviceidlab.provider.DeviceIdProvider.targetProcessDetectedFlow.collectAsState()

    // Real device identifiers state
    var realAndroidIdResult by remember { mutableStateOf(DeviceIdReader.readAndroidId(context)) }
    var realTelephonyResult by remember { mutableStateOf(DeviceIdReader.readTelephonyDeviceId(context)) }

    // Live Injection Test State
    var lastInjectionTestResult by remember { mutableStateOf<InjectionTestResult?>(null) }
    var isTestingInjection by remember { mutableStateOf(false) }

    // Sequential test ID index counter for runtime demonstration
    var testIdCounter by remember { mutableStateOf(1) }

    // Persistent Injected IDs State
    var injectAndroidIdInput by remember {
        mutableStateOf(
            DeviceIdReader.getSavedInjectedAndroidId(context).ifEmpty {
                "NPATCH_ANDROID_001"
            }
        )
    }
    var injectTelephonyIdInput by remember {
        mutableStateOf(
            DeviceIdReader.getSavedInjectedTelephonyId(context).ifEmpty {
                "NPATCH_TELEPHONY_001"
            }
        )
    }

    // NPatch 1.0.7 Runtime Injection Verification State
    var targetPackageInput by remember { mutableStateOf("com.example.deviceidlab") }
    var npatchVerificationDetails by remember { mutableStateOf<NpatchVerificationDetails?>(null) }
    var isVerifyingNpatch by remember { mutableStateOf(false) }

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
                                text = "NPatch 1.0.7 Dynamic Runtime Control",
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

            // PROMINENT SECTION: NPatch 1.0.7 Dynamic Hook Injection Lab
            item {
                NpatchInjectionTestCard(
                    targetPackage = targetPackageInput,
                    onTargetPackageChange = { targetPackageInput = it },
                    injectedAndroidId = injectAndroidIdInput,
                    onInjectedAndroidIdChange = {
                        injectAndroidIdInput = it
                        DeviceIdReader.saveInjectedAndroidId(context, it)
                    },
                    injectedTelephonyId = injectTelephonyIdInput,
                    onInjectedTelephonyIdChange = {
                        injectTelephonyIdInput = it
                        DeviceIdReader.saveInjectedTelephonyId(context, it)
                    },
                    currentAndroidTestId = providerAndroidTestId,
                    currentTelephonyTestId = providerTelephonyTestId,
                    lastInterceptedAndroidId = providerLastInterceptedAndroidId ?: "None",
                    lastInterceptedTelephonyId = providerLastInterceptedTelephonyId ?: "None",
                    lastTargetReadAndroid = providerLastTargetReadAndroid ?: "None",
                    lastTargetReadTelephony = providerLastTargetReadTelephony ?: "None",
                    isTargetDetected = isTargetProcessDetected,
                    onGenerateNextSequentialIdClick = {
                        testIdCounter++
                        val nextAndroidId = String.format(Locale.US, "NPATCH_ANDROID_%03d", testIdCounter)
                        val nextTelephonyId = String.format(Locale.US, "NPATCH_TELEPHONY_%03d", testIdCounter)
                        injectAndroidIdInput = nextAndroidId
                        injectTelephonyIdInput = nextTelephonyId
                        DeviceIdReader.saveInjectedIds(context, nextAndroidId, nextTelephonyId)
                        scope.launch {
                            snackbarHostState.showSnackbar("Runtime IDs updated (#$testIdCounter) - No repatching needed")
                        }
                    },
                    onGenerateRandomHexIdClick = {
                        val newRandomAndroidId = RandomIdGenerator.generateAndroidTestId(1L + (System.currentTimeMillis() % 1000000L))
                        val newRandomTelephonyId = RandomIdGenerator.generateTelephonyTestId(1L + (System.currentTimeMillis() % 1000000L))
                        injectAndroidIdInput = newRandomAndroidId
                        injectTelephonyIdInput = newRandomTelephonyId
                        DeviceIdReader.saveInjectedIds(context, newRandomAndroidId, newRandomTelephonyId)
                        scope.launch {
                            snackbarHostState.showSnackbar("Runtime IDs randomized - No repatching needed")
                        }
                    },
                    onLaunchTargetDemoClick = {
                        try {
                            val intent = Intent(context, TargetDemoActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (t: Throwable) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Launch error: ${t.message}")
                            }
                        }
                    },
                    verificationDetails = npatchVerificationDetails,
                    isVerifyingNpatch = isVerifyingNpatch,
                    onTestNpatchInjectionClick = {
                        isVerifyingNpatch = true
                        scope.launch {
                            val details = DeviceIdReader.verifyNpatchInjection(context, targetPackageInput)
                            npatchVerificationDetails = details
                            isVerifyingNpatch = false

                            val msg = if (details.isVerified) {
                                "VERIFIED: NPatch hook active for ${details.targetPackage}"
                            } else {
                                "INJECTION NOT DETECTED: Running in standard/unhooked mode"
                            }
                            snackbarHostState.showSnackbar(msg)
                        }
                    },
                    testResult = lastInjectionTestResult,
                    isTesting = isTestingInjection,
                    onTestInjectionClick = {
                        isTestingInjection = true
                        scope.launch {
                            // 1. Ensure target IDs are saved
                            DeviceIdReader.saveInjectedIds(context, injectAndroidIdInput, injectTelephonyIdInput)

                            // 2. Perform live injection verification
                            val result = DeviceIdReader.performInjectionTest(context, injectAndroidIdInput, targetPackageInput)
                            lastInjectionTestResult = result

                            // 3. Update real ID results in UI to match current state
                            realAndroidIdResult = DeviceIdReader.readAndroidId(context)
                            realTelephonyResult = DeviceIdReader.readTelephonyDeviceId(context)

                            isTestingInjection = false
                            val statusMsg = if (result.isSuccess) {
                                "SUCCESS: Android ID verified as ${result.currentId}"
                            } else {
                                "FAILED: ${result.failureReason ?: "Android ID unchanged"}"
                            }
                            snackbarHostState.showSnackbar(statusMsg)
                        }
                    }
                )
            }

            // Section 1: Real Device Identifiers (Queried directly from OS)
            item {
                RealDeviceIdentifiersCard(
                    androidIdResult = realAndroidIdResult,
                    telephonyResult = realTelephonyResult,
                    onRefresh = {
                        realAndroidIdResult = DeviceIdReader.readAndroidId(context)
                        realTelephonyResult = DeviceIdReader.readTelephonyDeviceId(context)
                        scope.launch {
                            snackbarHostState.showSnackbar("Real device identifiers refreshed from system")
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
                            lastInjectionTestResult = null
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

/**
 * Prominent NPatch 1.0.7 Injection Test and Dynamic Runtime Control Composable.
 * Shows Target Package configuration, Dynamic Test ID Generator (Sequential & Random Hex/IMEI),
 * Launch Target Demo App button, TEST NPATCH INJECTION, and exact verification diagnostics for BOTH
 * Android ID and Telephony Device ID.
 */
@Composable
fun NpatchInjectionTestCard(
    targetPackage: String,
    onTargetPackageChange: (String) -> Unit,
    injectedAndroidId: String,
    onInjectedAndroidIdChange: (String) -> Unit,
    injectedTelephonyId: String,
    onInjectedTelephonyIdChange: (String) -> Unit,
    currentAndroidTestId: String,
    currentTelephonyTestId: String,
    lastInterceptedAndroidId: String,
    lastInterceptedTelephonyId: String,
    lastTargetReadAndroid: String,
    lastTargetReadTelephony: String,
    isTargetDetected: Boolean,
    onGenerateNextSequentialIdClick: () -> Unit,
    onGenerateRandomHexIdClick: () -> Unit,
    onLaunchTargetDemoClick: () -> Unit,
    verificationDetails: NpatchVerificationDetails?,
    isVerifyingNpatch: Boolean,
    onTestNpatchInjectionClick: () -> Unit,
    testResult: InjectionTestResult?,
    isTesting: Boolean,
    onTestInjectionClick: () -> Unit
) {
    val isFrameworkHookActive = remember { DeviceIdReader.isNpatchHookActive() }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = when {
                    verificationDetails?.isVerified == true -> TagGreen
                    verificationDetails != null -> TagAmber
                    testResult?.isSuccess == true -> TagGreen
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                },
                shape = RoundedCornerShape(16.dp)
            )
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.section_npatch_test),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Framework Canary Hook Status Chip
                Surface(
                    color = if (isFrameworkHookActive || verificationDetails?.isVerified == true) {
                        TagGreen.copy(alpha = 0.15f)
                    } else {
                        TagAmber.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFrameworkHookActive || verificationDetails?.isVerified == true) TagGreen else TagAmber
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFrameworkHookActive || verificationDetails?.isVerified == true) "HOOK ACTIVE" else "STANDALONE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isFrameworkHookActive || verificationDetails?.isVerified == true) TagGreen else TagAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Runtime Architecture Info Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Single-patch architecture: Target APK is patched once. All new test IDs (Android ID & Telephony Device ID) update at runtime via ContentProvider IPC without repatching or reinstalling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Target Package Field
            OutlinedTextField(
                value = targetPackage,
                onValueChange = onTargetPackageChange,
                label = { Text(stringResource(R.string.label_target_package)) },
                placeholder = { Text("e.g. com.example.deviceidlab") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("target_package_input"),
                leadingIcon = {
                    Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Target Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = targetPackage == "com.example.deviceidlab",
                    onClick = { onTargetPackageChange("com.example.deviceidlab") },
                    label = { Text("Self (DeviceIdLab)") },
                    leadingIcon = if (targetPackage == "com.example.deviceidlab") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("chip_target_self")
                )
                FilterChip(
                    selected = targetPackage == "com.example.targetdemo",
                    onClick = { onTargetPackageChange("com.example.targetdemo") },
                    label = { Text("com.example.targetdemo") },
                    leadingIcon = if (targetPackage == "com.example.targetdemo") {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("chip_target_demo")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Current Runtime Injected Android ID Field
            OutlinedTextField(
                value = injectedAndroidId,
                onValueChange = onInjectedAndroidIdChange,
                label = { Text("1. Injected Android Test ID (Runtime IPC)") },
                placeholder = { Text("e.g. NPATCH_ANDROID_001 or 16-hex ID") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("injected_android_id_input"),
                leadingIcon = {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current Runtime Injected Telephony ID Field
            OutlinedTextField(
                value = injectedTelephonyId,
                onValueChange = onInjectedTelephonyIdChange,
                label = { Text("2. Injected Telephony Test ID (Runtime IPC)") },
                placeholder = { Text("e.g. NPATCH_TELEPHONY_001 or 15-digit IMEI") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("injected_telephony_id_input"),
                leadingIcon = {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Runtime ID Generation Buttons: Next Sequential & Random Hex
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGenerateNextSequentialIdClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("generate_next_sequential_id_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEXT TEST IDS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onGenerateRandomHexIdClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("generate_random_hex_id_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RANDOMIZE IDS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Live Target Launch & Testing Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onLaunchTargetDemoClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("launch_target_demo_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LAUNCH TARGET",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onTestInjectionClick,
                    enabled = !isTesting,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(46.dp)
                        .testTag("test_id_injection_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isTesting) {
                        Text("TESTING...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "TEST ID INJECTION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Diagnostic Verification Dashboard (Exact User Spec)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CodeBg)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = "--- NPATCH 1.0.7 RUNTIME DUAL-ID STATUS ---",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFA78BFA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MODULE STATUS:        ${if (NPatchHookEntry.isXposedEnvironmentActive || isFrameworkHookActive) "ACTIVE (Runtime Loaded)" else "INACTIVE / STANDALONE"}",
                        fontFamily = FontFamily.Monospace,
                        color = if (NPatchHookEntry.isXposedEnvironmentActive || isFrameworkHookActive) TagGreen else CodeText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "TARGET PROCESS:       ${if (isTargetDetected) "DETECTED (PID logged)" else "NOT DETECTED"}",
                        fontFamily = FontFamily.Monospace,
                        color = if (isTargetDetected) TagGreen else CodeText,
                        fontSize = 11.5.sp
                    )
                    Text(
                        text = "HOOK STATUS:          ${if (isFrameworkHookActive || testResult?.isSuccess == true || isTargetDetected) "ACTIVE (Settings.Secure + Telephony)" else "FAILED / PENDING"}",
                        fontFamily = FontFamily.Monospace,
                        color = if (isFrameworkHookActive || testResult?.isSuccess == true || isTargetDetected) TagGreen else CodeText,
                        fontSize = 11.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "--- [1] ANDROID ID VERIFICATION ---",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF38BDF8),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CURRENT TEST ID:      $currentAndroidTestId",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF60A5FA),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "LAST INTERCEPTED ID:  $lastInterceptedAndroidId",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "LAST TARGET READ:     $lastTargetReadAndroid",
                        fontFamily = FontFamily.Monospace,
                        color = if (lastTargetReadAndroid == currentAndroidTestId && lastTargetReadAndroid != "None") TagGreen else CodeText,
                        fontSize = 11.sp
                    )

                    val isAndroidPass = (testResult?.isSuccess == true) || (lastTargetReadAndroid == currentAndroidTestId && lastTargetReadAndroid != "None")
                    Text(
                        text = "ANDROID ID STATUS:    ${if (isAndroidPass) "PASS (REAL_HOOK_SUCCESS)" else if (testResult != null) "FAIL (${testResult.hookStatus})" else "PENDING"}",
                        fontFamily = FontFamily.Monospace,
                        color = if (isAndroidPass) TagGreen else if (testResult != null) Color(0xFFF87171) else CodeText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "--- [2] TELEPHONY DEVICE ID VERIFICATION ---",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF4ADE80),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CURRENT TEST ID:      $currentTelephonyTestId",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF4ADE80),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "LAST INTERCEPTED ID:  $lastInterceptedTelephonyId",
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "LAST TARGET READ:     $lastTargetReadTelephony",
                        fontFamily = FontFamily.Monospace,
                        color = if (lastTargetReadTelephony == currentTelephonyTestId && lastTargetReadTelephony != "None") TagGreen else CodeText,
                        fontSize = 11.sp
                    )

                    val isTelephonyPass = (lastTargetReadTelephony == currentTelephonyTestId && lastTargetReadTelephony != "None") ||
                            (lastInterceptedTelephonyId == currentTelephonyTestId && lastInterceptedTelephonyId != "None")
                    Text(
                        text = "TELEPHONY ID STATUS:  ${if (isTelephonyPass) "PASS (REAL_HOOK_SUCCESS)" else if (isTargetDetected) "RESTRICTED / PENDING QUERY" else "PENDING"}",
                        fontFamily = FontFamily.Monospace,
                        color = if (isTelephonyPass) TagGreen else if (isTargetDetected) Color(0xFFFBBF24) else CodeText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Results Display Area for Live ID Substitution (if test clicked)
            AnimatedVisibility(visible = testResult != null) {
                if (testResult != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp)),
                            color = if (testResult.isSuccess) TagGreen.copy(alpha = 0.15f) else TagRed.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (testResult.isSuccess) Icons.Default.CheckCircle else Icons.Default.Close,
                                    contentDescription = null,
                                    tint = if (testResult.isSuccess) TagGreen else TagRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (testResult.isSuccess) {
                                        "Target read verified: '${testResult.currentId}'"
                                    } else {
                                        testResult.failureReason ?: "Injection check failed."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (testResult.isSuccess) TagGreen else TagRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationRow(
    label: String,
    value: String,
    isPositive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (isPositive) TagGreen else Color(0xFFE2E8F0),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
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
                text = "Educational Laboratory demonstrating how Android APIs read device identifiers, platform security restrictions, deterministic 1M identity pool allocation, and NPatch 1.0.7 method interception.",
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
