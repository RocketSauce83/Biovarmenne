package com.rocketsauce83.biovarmennepro

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WizardPageTemplate(
    icon: ImageVector,
    title: String,
    description: String,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        content()
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WizardPageTemplate(
            icon = Icons.Default.Fingerprint,
            title = stringResource(id = R.string.app_name),
            description = stringResource(id = R.string.wizard_welcome_desc)
        )
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun BatteryPage(
    isOptimizationIgnored: Boolean,
    onOpenBatterySettings: () -> Unit,
    onNext: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        LaunchedEffect(isOptimizationIgnored) {
            if (isOptimizationIgnored) onNext()
        }

        WizardPageTemplate(
            icon = Icons.Default.BatteryChargingFull,
            title = stringResource(id = R.string.wizard_battery_title),
            description = stringResource(id = R.string.wizard_battery_desc)
        ) {
            Button(
                onClick = onOpenBatterySettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.button_battery_settings))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MiuiSettingsPage(
    onOpenAppInfoSettings: () -> Unit,
    onOpenAutostartSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WizardPageTemplate(
            icon = Icons.Default.SettingsSuggest,
            title = stringResource(id = R.string.wizard_miui_title),
            description = stringResource(id = R.string.wizard_miui_desc)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
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
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AccessibilityPage(
    isServiceEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onNext: () -> Unit
) {
    LaunchedEffect(isServiceEnabled) {
        if (isServiceEnabled) onNext()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        WizardPageTemplate(
            icon = Icons.Default.AccessibilityNew,
            title = stringResource(id = R.string.wizard_accessibility_title),
            description = stringResource(id = R.string.wizard_accessibility_desc)
        ) {
            Button(
                onClick = onOpenAccessibilitySettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AccessibilityNew, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.button_accessibility_settings))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PinPage(
    pinStorage: SecurePinStorage,
    onPinSaved: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var hasPin by remember { mutableStateOf(pinStorage.hasPin()) }
    var statusMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val ePinTooShort = stringResource(R.string.error_pin_too_short)
    val ePinMismatch = stringResource(R.string.error_pin_mismatch)
    val sPinSaved = stringResource(R.string.success_pin_saved)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        WizardPageTemplate(
            icon = Icons.Default.Password,
            title = stringResource(id = R.string.wizard_pin_title),
            description = stringResource(id = R.string.wizard_pin_desc)
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 8 && it.all { c -> c.isDigit() }) pin = it
                },
                label = { Text(stringResource(R.string.pin_input_label)) },
                supportingText = { Text(stringResource(R.string.pin_input_helper)) },
                leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 8 && it.all { c -> c.isDigit() }) confirmPin = it
                },
                label = { Text(stringResource(R.string.pin_confirm_label)) },
                leadingIcon = { Icon(Icons.Default.Password, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (statusMessage.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusMessage,
                        color = if (isError)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
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
                            onPinSaved()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (hasPin) stringResource(R.string.button_update_pin) else stringResource(R.string.button_save_pin))
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}