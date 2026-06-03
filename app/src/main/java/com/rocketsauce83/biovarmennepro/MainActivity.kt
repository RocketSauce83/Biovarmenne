package com.rocketsauce83.biovarmennepro

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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.rocketsauce83.biovarmennepro.ui.theme.BiovarmenneTheme
import kotlinx.coroutines.launch

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
                        appUpdateManager.completeUpdate()
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
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = "package:$packageName".toUri()
                        }
                        startActivity(intent)
                    },
                    onOpenAutostartSettings = {
                        try {
                            val intent = Intent()
                            intent.component = android.content.ComponentName(
                                "com.miui.securitycenter",
                                "com.miui.permcenter.autostart.AutoStartManagementActivity"
                            )
                            startActivity(intent)
                        } catch (_: Exception) {
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
        appUpdateManager.unregisterListener(installStateUpdatedListener)
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
fun StatusCard(
    text: String,
    isOk: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isOk)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isOk)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = if (isOk)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
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
    var isOnboardingCompleted by remember {
        mutableStateOf(pinStorage.isOnboardingCompleted())
    }

    if (isOnboardingCompleted) {
        MainStatusScreen(
            pinStorage = pinStorage,
            updateDownloaded = updateDownloaded,
            onCompleteUpdate = onCompleteUpdate,
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenBatterySettings = onOpenBatterySettings,
            onOpenAppInfoSettings = onOpenAppInfoSettings,
            onOpenAutostartSettings = onOpenAutostartSettings
        )
    } else {
        SetupWizardScreen(
            pinStorage = pinStorage,
            onFinishOnboarding = {
                pinStorage.setOnboardingCompleted(true)
                isOnboardingCompleted = true
            },
            onOpenAccessibilitySettings = onOpenAccessibilitySettings,
            onOpenBatterySettings = onOpenBatterySettings,
            onOpenAppInfoSettings = onOpenAppInfoSettings,
            onOpenAutostartSettings = onOpenAutostartSettings
        )
    }
}

enum class WizardPageType {
    WELCOME, BATTERY, MIUI_SETTINGS, ACCESSIBILITY, PIN_SETUP
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetupWizardScreen(
    pinStorage: SecurePinStorage,
    onFinishOnboarding: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppInfoSettings: () -> Unit,
    onOpenAutostartSettings: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val powerManager = context.getSystemService<PowerManager>()
    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
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

    val pages = remember {
        val list = mutableListOf(WizardPageType.WELCOME, WizardPageType.BATTERY)
        if (isMiui()) {
            list.add(WizardPageType.MIUI_SETTINGS)
        }
        list.add(WizardPageType.ACCESSIBILITY)
        list.add(WizardPageType.PIN_SETUP)
        list
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    val goToNextPage: () -> Unit = {
        if (pagerState.currentPage < pages.size - 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        } else {
            onFinishOnboarding()
        }
    }

    Scaffold(
        bottomBar = {
            WizardBottomBar(
                pagerState = pagerState,
                pageCount = pages.size,
                onSkip = onFinishOnboarding,
                onNext = goToNextPage
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            userScrollEnabled = false
        ) { pageIndex ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (pages[pageIndex]) {
                    WizardPageType.WELCOME -> WelcomePage()
                    WizardPageType.BATTERY -> BatteryPage(
                        isOptimizationIgnored = isBatteryOptimizationIgnored,
                        onOpenBatterySettings = onOpenBatterySettings,
                        onNext = goToNextPage
                    )
                    WizardPageType.MIUI_SETTINGS -> MiuiSettingsPage(
                        onOpenAppInfoSettings = onOpenAppInfoSettings,
                        onOpenAutostartSettings = onOpenAutostartSettings
                    )
                    WizardPageType.ACCESSIBILITY -> AccessibilityPage(
                        isServiceEnabled = isAccessibilityServiceEnabled,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onNext = goToNextPage
                    )
                    WizardPageType.PIN_SETUP -> PinPage(
                        pinStorage = pinStorage,
                        onPinSaved = goToNextPage
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WizardBottomBar(
    pagerState: PagerState,
    pageCount: Int,
    onSkip: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onSkip) {
            Text("Ohita")
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .background(color, CircleShape)
                )
            }
        }

        Button(onClick = onNext) {
            Text(if (pagerState.currentPage == pageCount - 1) "Valmis" else "Seuraava")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStatusScreen(
    pinStorage: SecurePinStorage,
    updateDownloaded: Boolean = false,
    onCompleteUpdate: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppInfoSettings: () -> Unit,
    onOpenAutostartSettings: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    val updDownloaded = stringResource(R.string.update_downloaded)
    val updRestart = stringResource(R.string.update_restart)
    val bPinCleared = stringResource(R.string.status_pin_cleared)

    LaunchedEffect(updateDownloaded) {
        if (updateDownloaded) {
            val result = snackbarHostState.showSnackbar(
                message = updDownloaded,
                actionLabel = updRestart,
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
    var showClearPinDialog by remember { mutableStateOf(false) }

    val isMiuiDevice = isMiui()
    val powerManager = context.getSystemService<PowerManager>()

    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false)
    }
    var isAccessibilityServiceEnabled by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    var isEnabled by remember { mutableStateOf(pinStorage.isEnabled()) }

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

    if (showClearPinDialog) {
        AlertDialog(
            onDismissRequest = { showClearPinDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_pin_title)) },
            text = { Text(stringResource(R.string.dialog_clear_pin_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pinStorage.clearPin()
                        hasPin = false
                        pin = ""
                        confirmPin = ""
                        statusMessage = bPinCleared
                        isError = false
                        showClearPinDialog = false
                    }
                ) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearPinDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
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
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.headlineMedium,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(R.string.app_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnabled)
                        stringResource(R.string.toggle_enabled)
                    else
                        stringResource(R.string.toggle_disabled),
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { enabled ->
                        isEnabled = enabled
                        pinStorage.setEnabled(enabled)
                    },
                    thumbContent = {
                        if (isEnabled) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            StatusCard(
                text = if (hasPin) stringResource(R.string.status_pin_saved)
                else stringResource(R.string.status_pin_missing),
                isOk = hasPin,
                icon = if (hasPin) Icons.Default.Lock else Icons.Default.LockOpen
            )

            Spacer(modifier = Modifier.height(8.dp))

            StatusCard(
                text = if (isBatteryOptimizationIgnored)
                    stringResource(R.string.status_battery_unrestricted)
                else
                    stringResource(R.string.status_battery_restricted),
                isOk = isBatteryOptimizationIgnored,
                icon = if (isBatteryOptimizationIgnored) Icons.Default.BatteryFull else Icons.Default.BatteryAlert
            )

            Spacer(modifier = Modifier.height(8.dp))

            StatusCard(
                text = if (isAccessibilityServiceEnabled)
                    stringResource(R.string.status_accessibility_enabled)
                else
                    stringResource(R.string.status_accessibility_disabled),
                isOk = isAccessibilityServiceEnabled,
                icon = Icons.Default.AccessibilityNew
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isMiuiDevice) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.status_popup_required_miui),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.status_autostart_required),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val ePinTooShort = stringResource(R.string.error_pin_too_short)
            val ePinMismatch = stringResource(R.string.error_pin_mismatch)
            val sPinSaved = stringResource(R.string.success_pin_saved)

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
                        if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
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
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.button_save_pin))
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (hasPin) {
                Button(
                    onClick = {
                        showClearPinDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_clear_pin))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!isBatteryOptimizationIgnored) {
                OutlinedButton(
                    onClick = onOpenBatterySettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_battery_settings))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!isAccessibilityServiceEnabled) {
                OutlinedButton(
                    onClick = { showDisclosureDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccessibilityNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_accessibility_settings))
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
                    Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_popup_settings))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenAutostartSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SettingsSuggest, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.button_autostart_settings))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}