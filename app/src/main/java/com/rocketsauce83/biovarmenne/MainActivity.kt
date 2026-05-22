package com.rocketsauce83.biovarmenne

import android.content.Intent
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
import com.rocketsauce83.biovarmenne.ui.theme.BiovarmenneTheme

class MainActivity : ComponentActivity() {

    private lateinit var pinStorage: SecurePinStorage

    private val batteryOptimizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // UI will recompose automatically when returning
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pinStorage = SecurePinStorage(this)
        enableEdgeToEdge()

        setContent {
            BiovarmenneTheme {
                BiovarmenneApp(
                    pinStorage = pinStorage,
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
                    }
                )
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: android.content.Context): Boolean {
    val accessibilityManager = context.getSystemService(
        android.content.Context.ACCESSIBILITY_SERVICE
    ) as android.view.accessibility.AccessibilityManager
    val enabledServices = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabledServices.contains(context.packageName)
}

@Composable
fun BiovarmenneApp(
    pinStorage: SecurePinStorage,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var hasPin by remember { mutableStateOf(pinStorage.hasPin()) }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showDisclosureDialog by remember { mutableStateOf(false) }

    val powerManager = context.getSystemService<PowerManager>()
    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        )
    }

    var isAccessibilityServiceEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }

    // Refresh battery status when app resumes
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isBatteryOptimizationIgnored =
                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
            }
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isBatteryOptimizationIgnored =
                    powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
                isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(context) // 👈 add this
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

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
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

            // PIN status card
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

            // Battery optimization status card
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

            // Accessibility status card
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

            Spacer(modifier = Modifier.height(32.dp))

            // PIN input fields
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
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.button_battery_settings))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isAccessibilityServiceEnabled) {
                Text(
                    text = stringResource(R.string.accessibility_reminder),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}