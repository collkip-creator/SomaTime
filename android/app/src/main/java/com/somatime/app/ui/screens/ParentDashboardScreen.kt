package com.somatime.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.somatime.app.data.AssignmentUploadParser
import com.somatime.app.data.CbcCurriculumRepository
import com.somatime.app.data.PaystackGatewayStatus
import com.somatime.app.data.PaystackInitResult
import com.somatime.app.data.PaystackVerifyResult
import com.somatime.app.data.SetupWizardTab
import com.somatime.app.data.SomaTimeStore
import com.somatime.app.data.SomaTimeUiState
import com.somatime.app.data.TeacherVerifyResult
import com.somatime.app.guard.ScreenTimeGuardService
import com.somatime.app.guard.UsageAccessHelper
import com.somatime.app.model.DeviceMode
import com.somatime.app.model.LearningArea
import com.somatime.app.model.LinkedTeacherSummary
import com.somatime.app.model.PendingApproval
import com.somatime.app.ui.theme.SomaAmber
import com.somatime.app.ui.theme.SomaEmerald
import com.somatime.app.ui.theme.SomaTeal
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onOpenSetupTab: (SetupWizardTab) -> Unit,
    onOpenTeacherPortal: () -> Unit,
    onLoadSampleDemo: () -> Unit,
    onRequestApprovalPin: (PendingApproval) -> Unit,
    onDeclineApproval: (String, String) -> Unit,
    onUpdateParentPin: (String) -> Boolean,
    onFetchPaystackStatus: suspend () -> PaystackGatewayStatus?,
    onInitializePaystack: suspend (String, List<String>) -> PaystackInitResult,
    onVerifyPaystack: suspend (String, List<String>, String) -> PaystackVerifyResult,
    onUpdateServerBaseUrl: (String) -> Boolean,
    onClearPaystackReturnNotice: () -> Unit,
    onVerifyTeacherCode: suspend (String) -> TeacherVerifyResult,
    onLinkTeacherToChild: (String, LinkedTeacherSummary) -> Unit,
    onUnlinkTeacherFromChild: (String, String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val child = uiState.selectedChild
    if (child == null) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Welcome to SomaTime · Fresh Start",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "All prior user data has been cleared. Register as a parent and set your own 4-digit PIN, pair a learner's device, or sign in as a teacher.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                        Button(
                            onClick = { onOpenSetupTab(SetupWizardTab.PARENT) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                        ) {
                            Text("1. Register Parent & Add Child")
                        }
                        OutlinedButton(
                            onClick = { onOpenSetupTab(SetupWizardTab.LEARNER) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("2. Pair Learner Phone with Code")
                        }
                        OutlinedButton(
                            onClick = { onOpenSetupTab(SetupWizardTab.TEACHER) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("3. Teacher Registration & Sign In")
                        }
                        HorizontalDivider()
                        TextButton(
                            onClick = onLoadSampleDemo,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Load Sample Demo Profiles (Optional)", color = SomaAmber)
                        }
                    }
                }
            }
        }
        return
    }
    var showAddDialog by remember { mutableStateOf(false) }

    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }
    var showPinFields by remember { mutableStateOf(false) }
    var showCurrentPin by remember { mutableStateOf(false) }
    var pinUpdateMsg by remember { mutableStateOf("") }
    var pinUpdateIsError by remember { mutableStateOf(false) }

    var payerPhone by remember(uiState.parentContact) {
        mutableStateOf(if ("@" !in uiState.parentContact) uiState.parentContact.trim() else "")
    }
    var paystackStatus by remember { mutableStateOf<PaystackGatewayStatus?>(null) }
    var isPayingPaystack by remember { mutableStateOf(false) }
    var isVerifyingPaystack by remember { mutableStateOf(false) }
    var pendingPaystackRef by remember { mutableStateOf<String?>(null) }
    var pendingPaystackChildIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingPaystackAmountKes by remember { mutableIntStateOf(CbcCurriculumRepository.MONTHLY_LICENSE_KES) }
    var licenseFeedback by remember { mutableStateOf("") }
    var licenseFeedbackIsError by remember { mutableStateOf(false) }
    var browserCheckoutUrl by remember { mutableStateOf("") }
    var showServerConfig by remember { mutableStateOf(false) }
    var serverUrlInput by remember(uiState.serverBaseUrl) { mutableStateOf(uiState.serverBaseUrl) }
    var serverUrlFeedback by remember { mutableStateOf("") }

    LaunchedEffect(uiState.serverBaseUrl) {
        paystackStatus = onFetchPaystackStatus()
    }

    LaunchedEffect(pendingPaystackRef) {
        val activeRef = pendingPaystackRef ?: return@LaunchedEffect
        repeat(60) {
            delay(3000L)
            if (pendingPaystackRef != activeRef) return@LaunchedEffect
            isVerifyingPaystack = true
            val res = onVerifyPaystack(
                activeRef,
                pendingPaystackChildIds,
                payerPhone.trim()
            )
            isVerifyingPaystack = false
            when (res.status) {
                "paid" -> {
                    pendingPaystackRef = null
                    pendingPaystackChildIds = emptyList()
                    licenseFeedbackIsError = false
                    licenseFeedback =
                        "Paystack M-Pesa confirmed KES ${res.amountKes}/mo · Ref ${res.receiptRef ?: activeRef}. Pairing & Child Lock unlocked ✓"
                    return@LaunchedEffect
                }
                "failed" -> {
                    pendingPaystackRef = null
                    pendingPaystackChildIds = emptyList()
                    licenseFeedbackIsError = true
                    licenseFeedback = res.message.ifBlank { "Paystack M-Pesa payment was cancelled or failed." }
                    return@LaunchedEffect
                }
            }
        }
    }

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
                        "Payment Method: Paystack M-Pesa Phone Prompt (KES $perChildKes/child/mo)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = SomaTeal
                    )

                    OutlinedTextField(
                        value = payerPhone,
                        onValueChange = {
                            payerPhone = it
                            licenseFeedback = ""
                            licenseFeedbackIsError = false
                        },
                        label = { Text("Safaricom M-Pesa Phone Number (e.g. 0712 345 678)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (uiState.paystackReturnNotice.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFF6EE7B7))
                        ) {
                            Text(
                                text = uiState.paystackReturnNotice,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanPhone = payerPhone.trim()
                            if (cleanPhone.filter(Char::isDigit).length < 9) {
                                licenseFeedbackIsError = true
                                licenseFeedback = "Enter a valid Kenyan M-Pesa phone number."
                                return@Button
                            }
                            if (isPayingPaystack) return@Button
                            isPayingPaystack = true
                            licenseFeedback = ""
                            licenseFeedbackIsError = false
                            browserCheckoutUrl = ""
                            onClearPaystackReturnNotice()
                            scope.launch {
                                val res = onInitializePaystack(cleanPhone, listOf(child.id))
                                isPayingPaystack = false
                                if (!res.ok && res.requiresBrowserAuth && res.browserCheckoutUrl.isNotBlank()) {
                                    browserCheckoutUrl = res.browserCheckoutUrl
                                    licenseFeedbackIsError = false
                                    licenseFeedback =
                                        "AI Studio preview requires browser Google session. Opening Chrome to send M-Pesa prompt to $cleanPhone..."
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(res.browserCheckoutUrl))
                                        )
                                    }
                                } else if (!res.ok) {
                                    licenseFeedbackIsError = true
                                    licenseFeedback = res.error.ifBlank { "Could not send Paystack M-Pesa phone prompt." }
                                } else if (res.mode == "paystack_test_demo" || res.mode == "paystack_stk_instant") {
                                    licenseFeedbackIsError = false
                                    licenseFeedback =
                                        "Paystack M-Pesa activated for ${child.name} (KES $perChildKes/mo · Ref ${res.reference}) ✓"
                                } else {
                                    pendingPaystackRef = res.reference
                                    pendingPaystackChildIds = listOf(child.id)
                                    pendingPaystackAmountKes = res.amountKes
                                    licenseFeedbackIsError = false
                                    licenseFeedback = res.message.ifBlank {
                                        "M-Pesa PIN prompt sent to $cleanPhone. Enter your M-Pesa PIN on your phone."
                                    }
                                }
                            }
                        },
                        enabled = !isPayingPaystack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (child.licenseActive) SomaTeal else Color(0xFF0F766E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            when {
                                isPayingPaystack -> "Sending M-Pesa Prompt..."
                                child.licenseActive -> "Renew ${child.name.split(" ").first()}'s License via Paystack (+30 Days · KES $perChildKes)"
                                else -> "Pay KES $perChildKes/mo via Paystack · ${child.name.split(" ").first()}"
                            }
                        )
                    }

                    if (unpaidChildren.size > 1) {
                        OutlinedButton(
                            onClick = {
                                val cleanPhone = payerPhone.trim()
                                if (cleanPhone.filter(Char::isDigit).length < 9) {
                                    licenseFeedbackIsError = true
                                    licenseFeedback = "Enter a valid Kenyan M-Pesa phone number."
                                    return@OutlinedButton
                                }
                                if (isPayingPaystack) return@OutlinedButton
                                isPayingPaystack = true
                                licenseFeedback = ""
                                licenseFeedbackIsError = false
                                browserCheckoutUrl = ""
                                onClearPaystackReturnNotice()
                                val targetIds = unpaidChildren.map { it.id }
                                scope.launch {
                                    val res = onInitializePaystack(cleanPhone, targetIds)
                                    isPayingPaystack = false
                                    if (!res.ok && res.requiresBrowserAuth && res.browserCheckoutUrl.isNotBlank()) {
                                        browserCheckoutUrl = res.browserCheckoutUrl
                                        licenseFeedbackIsError = false
                                        licenseFeedback =
                                            "AI Studio preview requires browser Google session. Opening Chrome to send M-Pesa prompt to $cleanPhone..."
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(res.browserCheckoutUrl))
                                            )
                                        }
                                    } else if (!res.ok) {
                                        licenseFeedbackIsError = true
                                        licenseFeedback = res.error.ifBlank { "Could not send Paystack M-Pesa phone prompt." }
                                    } else if (res.mode == "paystack_test_demo" || res.mode == "paystack_stk_instant") {
                                        licenseFeedbackIsError = false
                                        licenseFeedback =
                                            "All ${unpaidChildren.size} unpaid child licenses activated via Paystack (KES $unpaidTotalKes/mo · Ref ${res.reference}) ✓"
                                    } else {
                                        pendingPaystackRef = res.reference
                                        pendingPaystackChildIds = targetIds
                                        pendingPaystackAmountKes = res.amountKes
                                        licenseFeedbackIsError = false
                                        licenseFeedback = res.message.ifBlank {
                                            "M-Pesa PIN prompt sent to $cleanPhone. Enter your M-Pesa PIN on your phone."
                                        }
                                    }
                                }
                            },
                            enabled = !isPayingPaystack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pay All Unpaid via Paystack (${unpaidChildren.size} × KES $perChildKes = KES $unpaidTotalKes/mo)")
                        }
                    }

                    if (browserCheckoutUrl.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0F9FF),
                            border = BorderStroke(1.dp, Color(0xFF7DD3FC))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Authenticated Browser Checkout Ready",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0C4A6E)
                                )
                                Text(
                                    "The AI Studio preview server checks your Google login cookie in Chrome before triggering the Paystack M-Pesa STK prompt. Complete payment in Chrome, then tap Refresh if it doesn't auto-return.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF0369A1)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(browserCheckoutUrl))
                                                )
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1))
                                    ) {
                                        Text("Open M-Pesa Checkout in Browser")
                                    }
                                    OutlinedButton(
                                        onClick = { onRetryCloudSync() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Refresh Cloud License Status")
                                    }
                                }
                            }
                        }
                    }

                    val activePendingRef = pendingPaystackRef
                    if (!activePendingRef.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0F9FF),
                            border = BorderStroke(1.dp, Color(0xFF7DD3FC))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "M-Pesa Prompt Sent · KES $pendingPaystackAmountKes ($activePendingRef)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0C4A6E)
                                )
                                Text(
                                    "Enter your M-Pesa PIN on your phone. Verification runs automatically every 3 seconds.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF0369A1)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            if (isVerifyingPaystack) return@Button
                                            isVerifyingPaystack = true
                                            scope.launch {
                                                val res = onVerifyPaystack(
                                                    activePendingRef,
                                                    pendingPaystackChildIds,
                                                    payerPhone.trim()
                                                )
                                                isVerifyingPaystack = false
                                                when (res.status) {
                                                    "paid" -> {
                                                        pendingPaystackRef = null
                                                        pendingPaystackChildIds = emptyList()
                                                        licenseFeedbackIsError = false
                                                        licenseFeedback =
                                                            "Paystack confirmed KES ${res.amountKes}/mo · Ref ${res.receiptRef ?: activePendingRef} ✓"
                                                    }
                                                    "failed" -> {
                                                        pendingPaystackRef = null
                                                        pendingPaystackChildIds = emptyList()
                                                        licenseFeedbackIsError = true
                                                        licenseFeedback = res.message.ifBlank { "Payment failed or cancelled." }
                                                    }
                                                    else -> {
                                                        licenseFeedbackIsError = false
                                                        licenseFeedback = res.message.ifBlank { "Waiting for M-Pesa PIN confirmation..." }
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isVerifyingPaystack,
                                        modifier = Modifier.weight(2f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1))
                                    ) {
                                        Text(if (isVerifyingPaystack) "Checking..." else "Verify Now")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            pendingPaystackRef = null
                                            pendingPaystackChildIds = emptyList()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }

                    val statusText = when {
                        paystackStatus?.requiresBrowserAuth == true ->
                            "Paystack Gateway: Preview Proxy Requires Browser Session (Opens Browser for M-Pesa Prompt)"
                        paystackStatus?.configured == true && paystackStatus?.environment == "live" ->
                            "Paystack Gateway: Live Mode Active (M-Pesa + Card + Bank)"
                        paystackStatus?.configured == true ->
                            "Paystack Gateway: Test Mode Active (M-Pesa + Card + Bank)"
                        else ->
                            "Paystack Gateway: Checking Server (${uiState.serverBaseUrl})"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )

                    if (licenseFeedback.isNotEmpty()) {
                        Text(
                            text = licenseFeedback,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (!licenseFeedbackIsError) Color(0xFF065F46) else Color(0xFFB91C1C),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = { showServerConfig = !showServerConfig },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            if (showServerConfig) "Hide Gateway Server URL" else "Gateway Server URL (Optional)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (showServerConfig) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = serverUrlInput,
                                onValueChange = {
                                    serverUrlInput = it
                                    serverUrlFeedback = ""
                                },
                                label = { Text("Deployed or Emulator Backend URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = serverUrlInput.trim().trimEnd('/') == AssignmentUploadParser.DEFAULT_SERVER_URL,
                                    onClick = { serverUrlInput = AssignmentUploadParser.DEFAULT_SERVER_URL },
                                    label = { Text("Shared URL") }
                                )
                                FilterChip(
                                    selected = serverUrlInput.trim().trimEnd('/') == AssignmentUploadParser.DEV_SERVER_URL,
                                    onClick = { serverUrlInput = AssignmentUploadParser.DEV_SERVER_URL },
                                    label = { Text("Dev URL") }
                                )
                                FilterChip(
                                    selected = serverUrlInput.trim().trimEnd('/') == "http://10.0.2.2:3000",
                                    onClick = { serverUrlInput = "http://10.0.2.2:3000" },
                                    label = { Text("Emulator 10.0.2.2") }
                                )
                            }
                            Button(
                                onClick = {
                                    val saved = onUpdateServerBaseUrl(serverUrlInput)
                                    serverUrlFeedback = if (saved) {
                                        "Gateway URL saved ✓"
                                    } else {
                                        "Enter a valid https:// URL (or http://10.0.2.2:3000 for Emulator)."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                            ) {
                                Text("Save URL")
                            }
                            if (serverUrlFeedback.isNotEmpty()) {
                                Text(
                                    text = serverUrlFeedback,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (serverUrlFeedback.contains("✓")) SomaEmerald else Color(0xFFB91C1C)
                                )
                            }
                        }
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

                    if (child.deviceMode == DeviceMode.HAND_OFF || uiState.teacherAccount != null) {
                        HorizontalDivider()
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0FDFA),
                            border = BorderStroke(1.dp, Color(0xFF99F6E4))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "3-in-1 Single-Phone Mode (Parent + Teacher + Child)",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SomaTeal
                                )
                                Text(
                                    "1. Parent PIN protects settings & Child Lock exit.\n" +
                                        "2. Teacher: ${uiState.teacherAccount?.let { "${it.teacherCode} · ${it.school}" } ?: "Not linked yet"}\n" +
                                        "3. Child Hand-Off: No second phone or pairing code needed — tap Activate Child Lock above.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155)
                                )
                                Button(
                                    onClick = {
                                        if (uiState.teacherAccount != null) {
                                            onOpenTeacherPortal()
                                        } else {
                                            onOpenSetupTab(SetupWizardTab.TEACHER)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                                ) {
                                    Text(
                                        if (uiState.teacherAccount != null) {
                                            "Open Teacher Portal →"
                                        } else {
                                            "Link Teacher Profile on This Phone →"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Linked Class Teachers (Parent Opt-In Gate)
        item {
            var teacherCodeInput by remember(child.id) { mutableStateOf("") }
            var verifyingTeacher by remember(child.id) { mutableStateOf(false) }
            var verifiedTeacher by remember(child.id) { mutableStateOf<LinkedTeacherSummary?>(null) }
            var verifyFeedback by remember(child.id) { mutableStateOf("") }
            var verifyError by remember(child.id) { mutableStateOf(false) }

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
                        "Linked Class Teachers · ${child.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Only teachers whose exact Teacher Class Code you verify & link below can push priority assignments to ${child.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = teacherCodeInput,
                            onValueChange = {
                                teacherCodeInput = it.uppercase()
                                verifyFeedback = ""
                                verifiedTeacher = null
                            },
                            label = { Text("Teacher Class Code (e.g. TCH-7K4M9Q)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                if (verifyingTeacher || teacherCodeInput.isBlank()) return@Button
                                verifyingTeacher = true
                                verifyFeedback = ""
                                scope.launch {
                                    val res = onVerifyTeacherCode(teacherCodeInput)
                                    verifyingTeacher = false
                                    verifiedTeacher = res.teacher
                                    verifyFeedback = res.message
                                    verifyError = !res.ok
                                }
                            },
                            enabled = !verifyingTeacher && teacherCodeInput.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = SomaTeal)
                        ) {
                            Text(if (verifyingTeacher) "Checking..." else "Verify")
                        }
                    }

                    if (verifyFeedback.isNotEmpty() && verifiedTeacher == null) {
                        Text(
                            text = verifyFeedback,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (verifyError) Color(0xFFDC2626) else SomaEmerald
                        )
                    }

                    verifiedTeacher?.let { preview ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0FDFA),
                            border = BorderStroke(1.dp, SomaTeal)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "${preview.fullName} · ${preview.teacherCode}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SomaTeal
                                )
                                Text(
                                    "${preview.school} · Grade ${preview.grade} (${preview.classStream})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF334155)
                                )
                                if (preview.tscNumber.isNotEmpty()) {
                                    Text(
                                        "TSC / Staff No: ${preview.tscNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Text(
                                    "Subjects: ${preview.subjects.joinToString(", ") { it.displayName }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF0F172A)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            onLinkTeacherToChild(child.id, preview)
                                            teacherCodeInput = ""
                                            verifiedTeacher = null
                                            verifyFeedback = "Linked ${preview.fullName} (${preview.teacherCode}) to ${child.name}."
                                            verifyError = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SomaEmerald),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Confirm & Link Class")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            verifiedTeacher = null
                                            verifyFeedback = ""
                                        }
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            }
                        }
                    }

                    if (child.linkedTeacherCodes.isEmpty()) {
                        Text(
                            "No class teachers linked yet. Ask your child's teacher for their TCH-XXXXXX class code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            child.linkedTeacherCodes.forEach { linkedCode ->
                                val meta = child.linkedTeachers.firstOrNull { it.teacherCode == linkedCode }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (meta != null) "${meta.fullName} · $linkedCode" else linkedCode,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (meta != null) {
                                                    "${meta.school} · Grade ${meta.grade} · ${meta.subjects.joinToString(", ") { it.displayName }}"
                                                } else {
                                                    "Verified Class Code · Receiving assignments"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF475569)
                                            )
                                        }
                                        TextButton(
                                            onClick = { onUnlinkTeacherFromChild(child.id, linkedCode) }
                                        ) {
                                            Text("Unlink", color = Color(0xFFDC2626))
                                        }
                                    }
                                }
                            }
                        }
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
        var newSchool by remember { mutableStateOf("") }
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
                        label = { Text("School Name (e.g. Nairobi Primary School)") },
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
