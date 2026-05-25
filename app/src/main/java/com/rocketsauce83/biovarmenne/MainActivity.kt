package com.rocketsauce83.biovarmenne

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.google.android.play.core.appupdate.AppUpdateManager
import com.rocketsauce83.biovarmenne.ui.theme.BiovarmenneTheme
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus

class MainActivity : ComponentActivity() {

    private lateinit var pinStorage: SecurePinStorage
    private lateinit var appUpdateManager: AppUpdateManager

    private val updateDownloaded = mutableStateOf(false)

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            updateDownloaded.value = true
        }
    }

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) {
        // Update result handled — no action needed on cancel or failure
    }

    @SuppressLint("BatteryLife")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinStorage = SecurePinStorage(this)
        pinStorage.migrateIfNeeded()
        enableEdgeToEdge()
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateUpdatedListener)
        checkForUpdates()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }

        setContent {
            BiovarmenneTheme {
                BiovarmenneApp(
                    pinStorage = pinStorage,
                    updateDownloaded = updateDownloaded.value,
                    onCompleteUpdate = {
                        appUpdateManager.completeUpdate()       // 👈 pass action
                    },
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onOpenBatterySettings = {
                        batteryOptimizationLauncher.launch(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                "package:$packageName".toUri()
                            )
                        )
                    },
                    onOpenAppInfoSettings = {
                        // Navigates directly to the App Info screen shown in your screenshot
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:$packageName".toUri()
                        }
                        startActivity(intent)
                    },
                    onOpenAutostartSettings = { // 👈 Add this block
                        try {
                            val intent = Intent()
                            intent.component = android.content.ComponentName(
                                "com.miui.securitycenter",
                                "com.miui.permcenter.autostart.AutoStartManagementActivity"
                            )
                            startActivity(intent)
                        } catch (_: Exception) {
                            // Fallback to standard app settings if the MIUI activity isn't found
                            startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    "package:$packageName".toUri()
                                )
                            )
                        }
                    }
                )
            }
        }
    }

    private fun checkForUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() ==
                UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isFlexibleUpdateAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                updateDownloaded.value = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener) // 👈 unregister
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(context.packageName)
}

private fun isMiui(): Boolean {
    return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
            Build.BRAND.equals("Xiaomi", ignoreCase = true) ||
            Build.BRAND.equals("POCO", ignoreCase = true) ||
            Build.BRAND.equals("Redmi", ignoreCase = true)
}

@Composable
fun BiovarmenneApp(
    pinStorage: SecurePinStorage,
    updateDownloaded: Boolean = false,
    onCompleteUpdate: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppInfoSettings: () -> Unit,
    onOpenAutostartSettings: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val updDownloaded = stringResource(R.string.update_downloaded)
    val updRestart = stringResource(R.string.update_restart)

    LaunchedEffect(updateDownloaded) {
        if (updateDownloaded) {
            val result = snackbarHostState.showSnackbar(
                message = (updDownloaded),
                actionLabel = (updRestart),
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                onCompleteUpdate()
            }
        }
    }

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var hasPin by remember { mutableStateOf(pinStorage.hasPin()) }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showDisclosureDialog by remember { mutableStateOf(false) }

    val isMiuiDevice = isMiui()

    val powerManager = context.getSystemService<PowerManager>()
    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        )
    }

    var isAccessibilityServiceEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isBatteryOptimizationIgnored =
                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
                isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDisclosureDialog) {
        AlertDialog(
            onDismissRequest = { showDisclosureDialog = false },
            title = { Text(stringResource(R.string.dialog_accessibility_title)) },
            text = { Text(stringResource(R.string.dialog_accessibility_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosureDialog = false
                    onOpenAccessibilitySettings()
                }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisclosureDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 32.sp,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.app_subtitle),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasPin)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = if (hasPin) stringResource(R.string.status_pin_saved)
                    else stringResource(R.string.status_pin_missing),
                    modifier = Modifier.padding(16.dp),
                    color = if (hasPin)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isBatteryOptimizationIgnored)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = if (isBatteryOptimizationIgnored)
                        stringResource(R.string.status_battery_unrestricted)
                    else
                        stringResource(R.string.status_battery_restricted),
                    modifier = Modifier.padding(16.dp),
                    color = if (isBatteryOptimizationIgnored)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityServiceEnabled)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = if (isAccessibilityServiceEnabled)
                        stringResource(R.string.status_accessibility_enabled)
                    else
                        stringResource(R.string.status_accessibility_disabled),
                    modifier = Modifier.padding(16.dp),
                    color = if (isAccessibilityServiceEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MIUI info cards — use tertiaryContainer color
            if (isMiuiDevice) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer  // 👈 different color
                    )
                ) {
                    Text(
                        text = stringResource(R.string.status_popup_required_miui),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer  // 👈 different color
                    )
                ) {
                    Text(
                        text = stringResource(R.string.status_autostart_required),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val ePinTooShort = stringResource(R.string.error_pin_too_short)
            val ePinMismatch = stringResource(R.string.error_pin_mismatch)
            val sPinSaved = stringResource(R.string.success_pin_saved)
            val bPinCleared = stringResource(R.string.status_pin_cleared)

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it
                },
                label = { Text(stringResource(R.string.pin_input_label)) },
                supportingText = { Text(stringResource(R.string.pin_input_helper)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 8 && it.all { c -> c.isDigit() }) confirmPin = it
                },
                label = { Text(stringResource(R.string.pin_confirm_label)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    color = if (isError)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    when {
                        pin.length < 4 -> {
                            statusMessage = ePinTooShort
                            isError = true
                        }
                        pin != confirmPin -> {
                            statusMessage = ePinMismatch
                            isError = true
                        }
                        else -> {
                            pinStorage.savePin(pin)
                            hasPin = true
                            pin = ""
                            confirmPin = ""
                            statusMessage = sPinSaved
                            isError = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_save_pin))
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (hasPin) {
                OutlinedButton(
                    onClick = {
                        pinStorage.clearPin()
                        hasPin = false
                        pin = ""
                        confirmPin = ""
                        statusMessage = bPinCleared
                        isError = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_clear_pin))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!isAccessibilityServiceEnabled) {
                OutlinedButton(
                    onClick = { showDisclosureDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_accessibility_settings))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!isBatteryOptimizationIgnored) {
                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_battery_settings))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!isAccessibilityServiceEnabled) {
                Text(
                    text = stringResource(R.string.accessibility_reminder),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (isMiuiDevice) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenAppInfoSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_popup_settings))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenAutostartSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_autostart_settings))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}