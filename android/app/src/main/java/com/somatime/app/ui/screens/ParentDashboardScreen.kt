package com.somatime.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.SomaTimeStore
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.guard.ScreenTimeGuardService
import com.somatime.app.guard.UsageAccessHelper
import com.somatime.app.model.DeviceMode
import com.somatime.app.model.LearningArea
import com.somatime.app.model.PendingApproval
import com.somatime.app.ui.theme.SomaEmerald
import com.somatime.app.ui.theme.SomaTeal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentDashboardScreen(
    uiState: SomaTimeUiState,
    onSelectChild: (String) -> Unit,
    onUpdateChildCap: (String, Int) -> Unit,
    onUpdateExchangeRate: (String, Int) -> Unit,
    onToggleMandatory: (String, LearningArea) -> Unit,
    onSetApprovalRequired: (String, Boolean) -> Unit,
    onSetDeviceMode: (String, DeviceMode) -> Unit,
    onActivateHandOff: (String) -> Unit,
    onAddChild: (String, Int, String) -> Unit,
    onRegeneratePairingCode: (String) -> Unit,
    onRetryCloudSync: () -> Unit,
    onOpenSetupWizard: () -> Unit,
    onRequestApprovalPin: (PendingApproval) -> Unit,
    onDeclineApproval: (String, String) -> Unit,
    onUpdateParentPin: (String) -> Boolean,
    onActivateChildLicense: (String, String) -> Unit,
    onActivateAllUnpaidLicenses: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val child = uiState.selectedChild
    var showAddDialog by remember { mutableStateOf(false) }

    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var showPinFields by remember { mutableStateOf(false) }
    var showCurrentPin by remember { mutableStateOf(false) }
    var pinUpdateMsg by remember { mutableStateOf("") }
    var pinUpdateIsError by remember { mutableStateOf(false) }

    var mpesaPhone by remember(uiState.parentContact) { mutableStateOf(uiState.parentContact) }
    var licenseFeedback by remember { mutableStateOf("") }

    var hasUsageStats by remember { mutableStateOf(UsageAccessHelper.hasUsageStatsPermission(context)) }
    var hasOverlay by remember { mutableStateOf(UsageAccessHelper.canDrawOverlays(context)) }
    var hasAccessibility by remember { mutableStateOf(UsageAccessHelper.isAccessibilityServiceEnabled(context)) }
    var hasDeviceAdmin by remember { mutableStateOf(UsageAccessHelper.isDeviceAdminActive(context)) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = UsageAccessHelper.hasUsageStatsPermission(context)
                hasOverlay = UsageAccessHelper.canDrawOverlays(context)
                hasAccessibility = UsageAccessHelper.isAccessibilityServiceEnabled(context)
                hasDeviceAdmin = UsageAccessHelper.isDeviceAdminActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Parent & Guardian Center",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Kenyan CBC rules, screen-time limits & real device guard",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = onOpenSetupWizard) {
                        Icon(Icons.Rounded.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Setup")
                    }
                    OutlinedButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Child")
                    }
                }
            }
        }

        // Child Profile Switcher
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.children.forEach { profile ->
                    val selected = profile.id == child.id
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectChild(profile.id) },
                        label = {
                            val badge = if (profile.licenseActive) "Licensed" else "Unpaid"
                            Text(
                                "${profile.name} · G${profile.grade} · $badge (${SomaTimeStore.formatClock(profile.availableScreenSeconds)})"
                            )
                        }
                    )
                }
            }
        }

        // Licensing Area (Per Child Per Month)
        item {
            val unpaidChildren = uiState.children.filter { !it.licenseActive }
            val activeCount = uiState.children.size - unpaidChildren.size
            val perChildKes = CbcCurriculumRepository.MONTHLY_LICENSE_KES
            val unpaidTotalKes = unpaidChildren.size * perChildKes

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (child.licenseActive) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                ),
                border = BorderStroke(
                    1.dp,
                    if (child.licenseActive) SomaEmerald else Color(0xFFF59E0B)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (child.licenseActive) Icons.Rounded.Verified else Icons.Rounded.Payments,
                                contentDescription = null,
                                tint = if (child.licenseActive) SomaEmerald else Color(0xFFD97706)
                            )
                            Column {
                                Text(
                                    "Licensing Area · ${child.name.split(" ").first()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "KES $perChildKes / child / month · $activeCount/${uiState.children.size} children licensed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (child.licenseActive) SomaEmerald else Color(0xFFD97706)
                        ) {
                            Text(
                                text = if (child.licenseActive) "ACTIVE" else "UNPAID",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        if (child.licenseActive) {
                            "License active (${child.licensePaymentRef ?: "Verified"}) · Expires ${(child.licenseExpiresAt ?: "").take(10).ifEmpty { "30 days" }}. Pairing codes and Child Lock Hand-Off are unlocked."
                        } else {
                            "Pay KES $perChildKes/month for ${child.name} to generate phone pairing codes and activate \"Activate Child Lock & Hand Over Phone\" on a shared device."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                    )

                    // Per-child status breakdown
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        uiState.children.forEach { profile ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${profile.name} (G${profile.grade})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    if (profile.licenseActive) {
                                        "Active · ${profile.licensePaymentRef ?: "Paid"}"
                                    } else {
                                        "KES $perChildKes/mo required"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (profile.licenseActive) SomaEmerald else Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "Payment Method: M-Pesa Only · Paybill 174379 (Safaricom Daraja STK)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SomaEmerald
                    )

                    OutlinedTextField(
                        value = mpesaPhone,
                        onValueChange = {
                            mpesaPhone = it
                            licenseFeedback = ""
                        },
                        label = { Text("Safaricom M-Pesa Phone Number (e.g. 0712 345 678)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            val digits = mpesaPhone.filter(Char::isDigit)
                            if (digits.length < 9) {
                                licenseFeedback = "Enter a valid Safaricom M-Pesa phone number."
                                return@Button
                            }
                            onActivateChildLicense(child.id, mpesaPhone)
                            licenseFeedback = "M-Pesa license activated for ${child.name} (KES $perChildKes/mo). Pairing & Child Lock unlocked ✓"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (child.licenseActive) SomaTeal else Color(0xFF059669)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (child.licenseActive) {
                                "Renew ${child.name.split(" ").first()}'s License via M-Pesa (+30 Days · KES $perChildKes)"
                            } else {
                                "Pay KES $perChildKes/mo via M-Pesa · ${child.name.split(" ").first()}"
                            }
                        )
                    }

                    if (unpaidChildren.size > 1) {
                        OutlinedButton(
                            onClick = {
                                val digits = mpesaPhone.filter(Char::isDigit)
                                if (digits.length < 9) {
                                    licenseFeedback = "Enter your Safaricom M-Pesa number first."
                                    return@OutlinedButton
                                }
                                onActivateAllUnpaidLicenses(mpesaPhone)
                                licenseFeedback = "All ${unpaidChildren.size} unpaid child licenses activated via M-Pesa (KES $unpaidTotalKes/mo) ✓"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pay All Unpaid via M-Pesa (${unpaidChildren.size} × KES $perChildKes = KES $unpaidTotalKes/mo)")
                        }
                    }

                    if (licenseFeedback.isNotEmpty()) {
                        Text(
                            text = licenseFeedback,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (child.licenseActive) Color(0xFF065F46) else Color(0xFFB91C1C),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Custom Parent PIN Setup & Security Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Parent Security & Custom PIN",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (uiState.parentPin == CbcCurriculumRepository.PARENT_PIN) {
                                    "Using default PIN · Set your own 4-digit PIN below"
                                } else {
                                    "Custom 4-digit Parent PIN active (${if (showCurrentPin) uiState.parentPin else "••••"})"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.parentPin == CbcCurriculumRepository.PARENT_PIN) {
                                    Color(0xFFD97706)
                                } else {
                                    SomaEmerald
                                }
                            )
                        }
                        IconButton(onClick = { showCurrentPin = !showCurrentPin }) {
                            Icon(
                                if (showCurrentPin) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showCurrentPin) "Hide current PIN" else "Show current PIN"
                            )
                        }
                    }

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { input ->
                            newPinInput = input.filter { it.isDigit() }.take(4)
                            pinUpdateMsg = ""
                        },
                        label = { Text("New 4-Digit Parent PIN") },
                        visualTransformation = if (showPinFields) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPinFields = !showPinFields }) {
                                Icon(
                                    if (showPinFields) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (showPinFields) "Hide PIN" else "Show PIN"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = confirmPinInput,
                        onValueChange = { input ->
                            confirmPinInput = input.filter { it.isDigit() }.take(4)
                            pinUpdateMsg = ""
                        },
                        label = { Text("Confirm New 4-Digit PIN") },
                        visualTransformation = if (showPinFields) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (pinUpdateMsg.isNotEmpty()) {
                        Text(
                            text = pinUpdateMsg,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (pinUpdateIsError) Color(0xFFDC2626) else SomaEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            if (newPinInput.length != 4) {
                                pinUpdateIsError = true
                                pinUpdateMsg = "PIN must be exactly 4 digits."
                                return@Button
                            }
                            if (newPinInput != confirmPinInput) {
                                pinUpdateIsError = true
                                pinUpdateMsg = "New PIN and Confirm PIN do not match."
                                return@Button
                            }
                            val ok = onUpdateParentPin(newPinInput)
                            if (ok) {
                                pinUpdateIsError = false
                                pinUpdateMsg = "Custom Parent PIN updated and synced to Cloud ✓"
                                newPinInput = ""
                                confirmPinInput = ""
                            } else {
                                pinUpdateIsError = true
                                pinUpdateMsg = "Could not save PIN. Enter 4 digits."
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                    ) {
                        Icon(Icons.Rounded.Key, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Custom Parent PIN")
                    }
                }
            }
        }

        // Real Android Guard Permissions Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, tint = SomaTeal)
                        Text(
                            "Real Android App Interception Guard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        "Enable all 4 protections on the child's phone so SomaTime blocks YouTube/TikTok, disables swiping down notifications, blocks Home/Recents/Settings, and prevents uninstall.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { UsageAccessHelper.openUsageAccessSettings(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (hasUsageStats) "Usage Access ✓" else "1. Usage Access")
                        }
                        OutlinedButton(
                            onClick = { UsageAccessHelper.openOverlaySettings(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (hasOverlay) "Overlay ✓" else "2. Overlay")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { UsageAccessHelper.openAccessibilitySettings(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (hasAccessibility) "Swipe/Btn Guard ✓" else "3. Lockdown Guard")
                        }
                        OutlinedButton(
                            onClick = { UsageAccessHelper.openDeviceAdminSettings(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (hasDeviceAdmin) "Uninstall Lock ✓" else "4. Anti-Uninstall")
                        }
                    }
                    Button(
                        onClick = {
                            if (uiState.guardServiceRunning) {
                                ScreenTimeGuardService.stop(context)
                            } else {
                                ScreenTimeGuardService.start(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.guardServiceRunning) Color(0xFF334155) else SomaEmerald
                        )
                    ) {
                        Text(
                            if (uiState.guardServiceRunning) {
                                "Stop Background Guard Service"
                            } else {
                                "Start Background Guard Service"
                            }
                        )
                    }
                }
            }
        }

        // Device Mode & Hand-Off Lock Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Device Mode · ${child.name} (Grade ${child.grade})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${CbcCurriculumRepository.levelGroupForGrade(child.grade)} · ${child.school}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = child.deviceMode == DeviceMode.OWN_PHONE,
                            onClick = { onSetDeviceMode(child.id, DeviceMode.OWN_PHONE) },
                            label = { Text("Child's Own Phone") }
                        )
                        FilterChip(
                            selected = child.deviceMode == DeviceMode.HAND_OFF,
                            onClick = { onSetDeviceMode(child.id, DeviceMode.HAND_OFF) },
                            label = { Text("Shared Hand-Off Mode") }
                        )
                    }
                    Button(
                        onClick = { onActivateHandOff(child.id) },
                        enabled = child.licenseActive,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SomaTeal),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Activate Child Lock & Hand Over Phone")
                    }
                    Text(
                        if (child.licenseActive) {
                            "Locks immediately into Child Mode. Your Parent PIN is required to exit."
                        } else {
                            "Monthly license required (KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo) to activate Child Lock & Hand-Off Mode."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (child.licenseActive) Color(0xFF64748B) else Color(0xFFB45309)
                    )

                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Child Phone Pairing Code",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                when {
                                    !child.licenseActive -> "Pay KES ${CbcCurriculumRepository.MONTHLY_LICENSE_KES}/mo license above to unlock & generate pairing code"
                                    child.isPaired -> "Paired · Background watch-minute guard active"
                                    else -> "Not paired yet · Enter this code on child's phone"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    !child.licenseActive -> Color(0xFFB45309)
                                    child.isPaired -> SomaEmerald
                                    else -> Color(0xFF64748B)
                                }
                            )
                        }
                        Text(
                            text = if (child.licenseActive) child.pairingCode else "SOMA-••••",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (child.licenseActive) SomaTeal else Color(0xFF94A3B8)
                        )
                    }

                    Text(
                        text = uiState.cloudSyncMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.cloudSyncOk) SomaEmerald else Color(0xFFDC2626),
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onRegeneratePairingCode(child.id) },
                            enabled = child.licenseActive,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Code")
                        }
                        OutlinedButton(
                            onClick = onRetryCloudSync,
                            enabled = child.licenseActive,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sync Cloud")
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenSetupWizard,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Setup & Pair Learner Wizard")
                    }
                }
            }
        }

        // Screen Time Economy & Rules Engine
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Screen Time Economy & Rules",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Max Cap", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${child.dailyCapMins} mins/day",
                            fontWeight = FontWeight.Bold,
                            color = SomaTeal
                        )
                    }
                    Slider(
                        value = child.dailyCapMins.toFloat(),
                        onValueChange = { onUpdateChildCap(child.id, it.toInt()) },
                        valueRange = 30f..180f,
                        steps = 9
                    )

                    Text("CBC Exchange Rate (1 Activity =)", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10, 15, 20, 30).forEach { mins ->
                            FilterChip(
                                selected = child.exchangeRateMins == mins,
                                onClick = { onUpdateExchangeRate(child.id, mins) },
                                label = { Text("${mins}m") }
                            )
                        }
                    }

                    HorizontalDivider()
                    Text("Mandatory Focus Subjects", style = MaterialTheme.typography.bodyMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val gradeSubjects = (CbcCurriculumRepository.subjectsForGrade(child.grade) + child.mandatorySubjects).distinct()
                        gradeSubjects.forEach { area ->
                            FilterChip(
                                selected = area in child.mandatorySubjects,
                                onClick = { onToggleMandatory(child.id, area) },
                                label = { Text(area.displayName) }
                            )
                        }
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Require Parent PIN Approval", fontWeight = FontWeight.SemiBold)
                            Text(
                                if (child.requireParentApproval) "Earned minutes wait in queue" else "Auto-approve earned minutes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Switch(
                            checked = child.requireParentApproval,
                            onCheckedChange = { onSetApprovalRequired(child.id, it) }
                        )
                    }
                }
            }
        }

        // Pending Approvals Queue
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Parent Approval Queue (${child.pendingApprovals.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (child.pendingApprovals.isEmpty()) {
                        Text(
                            "No pending minute requests.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        child.pendingApprovals.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.lessonTitle, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${item.learningArea.displayName} · ${item.scorePercent}% · +${item.minutesEarned}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(onClick = { onDeclineApproval(child.id, item.id) }) {
                                        Text("Decline")
                                    }
                                    Button(
                                        onClick = { onRequestApprovalPin(item) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // CBC Progress & Competency Analytics
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "CBC Competency Analytics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Completed: ${child.completedLessonIds.size} lessons · Learning: ${child.learnedMinutesToday}m · Play: ${child.usedScreenTodayMins}/${child.dailyCapMins}m",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                    LinearProgressIndicator(
                        progress = {
                            (child.usedScreenTodayMins.toFloat() / child.dailyCapMins.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    child.rubricByArea.forEach { (area, rubric) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(area.displayName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${rubric.name} · ${rubric.label}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SomaTeal
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showAddDialog) {
        var newName by remember { mutableStateOf("") }
        var newSchool by remember { mutableStateOf("Nairobi Primary School") }
        var newGrade by remember { mutableIntStateOf(5) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Child Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Child Full Name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newSchool,
                        onValueChange = { newSchool = it },
                        label = { Text("School Name") },
                        singleLine = true
                    )
                    Text("Grade: $newGrade (${CbcCurriculumRepository.levelGroupForGrade(newGrade)})")
                    Slider(
                        value = newGrade.toFloat(),
                        onValueChange = { newGrade = it.toInt() },
                        valueRange = 1f..12f,
                        steps = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onAddChild(newName, newGrade, newSchool)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                ) {
                    Text("Save Child")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
