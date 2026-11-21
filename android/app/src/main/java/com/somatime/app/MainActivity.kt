package com.somatime.app

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.somatime.app.data.SetupWizardTab
import com.somatime.app.data.SomaTimeStore
import com.somatime.app.guard.ScreenTimeGuardService
import com.somatime.app.guard.UsageAccessHelper
import com.somatime.app.model.PendingApproval
import com.somatime.app.model.RoleView
import com.somatime.app.ui.components.PinDialog
import com.somatime.app.ui.screens.ChildModeScreen
import com.somatime.app.ui.screens.ParentDashboardScreen
import com.somatime.app.ui.screens.SetupWizardScreen
import com.somatime.app.ui.screens.TeacherPortalScreen
import com.somatime.app.ui.theme.SomaTeal
import com.somatime.app.ui.theme.SomaTimeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private sealed interface PinIntent {
    data class ExitHandOff(val targetRole: RoleView) : PinIntent
    data object UnlockChild : PinIntent
    data class ApproveMinutes(val approval: PendingApproval) : PinIntent
}

class MainActivity : ComponentActivity() {

    @Volatile
    private var takeoverLockEnforced: Boolean = false
    private var isActivityResumed: Boolean = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled gracefully */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val store = SomaTimeStore.getInstance(this)
        handleIncomingIntent(intent, store)

        setContent {
            SomaTimeTheme {
                SomaTimeRoot(
                    store = store,
                    onTakeoverLockChanged = { locked ->
                        takeoverLockEnforced = locked
                        applyTakeoverSystemUi(locked)
                    },
                    onMinimizeAndResumeApp = {
                        minimizeAndResumeInterruptedApp(store)
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent, SomaTimeStore.getInstance(this))
    }

    override fun onResume() {
        super.onResume()
        isActivityResumed = true
        applyTakeoverSystemUi(takeoverLockEnforced)
    }

    override fun onPause() {
        isActivityResumed = false
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (takeoverLockEnforced) {
            hideSystemBars()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (takeoverLockEnforced) {
            relaunchToFront()
        }
    }

    private fun isBlockedHardwareKey(keyCode: Int): Boolean = keyCode in setOf(
        KeyEvent.KEYCODE_APP_SWITCH,
        KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_SEARCH,
        KeyEvent.KEYCODE_VOLUME_UP,
        KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_CAMERA,
        KeyEvent.KEYCODE_VOICE_ASSIST,
        KeyEvent.KEYCODE_ASSIST
    )

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (takeoverLockEnforced && isBlockedHardwareKey(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (takeoverLockEnforced && isBlockedHardwareKey(keyCode)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun isActuallyInLockTaskMode(): Boolean {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    private fun applyTakeoverSystemUi(locked: Boolean) {
        if (locked) {
            hideSystemBars()
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            // Pin ONLY when the learner interruption popup is actually foregrounded and resumed
            if (isActivityResumed && !isActuallyInLockTaskMode()) {
                runCatching { startLockTask() }
            }
        } else {
            if (isActuallyInLockTaskMode()) {
                runCatching { stopLockTask() }
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            showSystemBars()
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun relaunchToFront() {
        val lockIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        runCatching { startActivity(lockIntent) }
    }

    private fun handleIncomingIntent(intent: Intent?, store: SomaTimeStore) {
        val interceptedPkg = intent?.getStringExtra(ScreenTimeGuardService.EXTRA_INTERCEPTED_APP)
        if (!interceptedPkg.isNullOrBlank()) {
            store.updateForegroundApp(interceptedPkg)
            store.setRoleView(RoleView.CHILD)
        }
    }

    private fun minimizeAndResumeInterruptedApp(store: SomaTimeStore) {
        takeoverLockEnforced = false
        store.suppressGuardBriefly(2500L)
        applyTakeoverSystemUi(false)

        val targetPkg = store.state.value.lastInterruptedAppPackage
        val launchIntent = if (UsageAccessHelper.isTrackableInterruptedApp(this, targetPkg)) {
            targetPkg?.let { packageManager.getLaunchIntentForPackage(it) }
        } else {
            null
        }

        if (launchIntent != null) {
            launchIntent.flags =
                (launchIntent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK.inv() and Intent.FLAG_ACTIVITY_CLEAR_TOP.inv()) or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            runCatching { startActivity(launchIntent) }
        }
        moveTaskToBack(true)
    }
}

@Composable
private fun SomaTimeRoot(
    store: SomaTimeStore,
    onTakeoverLockChanged: (Boolean) -> Unit,
    onMinimizeAndResumeApp: () -> Unit
) {
    val context = LocalContext.current
    val uiState by store.state.collectAsState()
    var pinIntent by remember { mutableStateOf<PinIntent?>(null) }

    LaunchedEffect(uiState.roleView, uiState.selectedChild.id, uiState.selectedChild.isPaired, uiState.handOffActive) {
        if (uiState.roleView == RoleView.CHILD &&
            (uiState.selectedChild.isPaired || uiState.handOffActive) &&
            !uiState.guardServiceRunning
        ) {
            runCatching { ScreenTimeGuardService.start(context) }
        }
    }

    LaunchedEffect(uiState.roleView, uiState.selectedChild.id) {
        while (isActive) {
            runCatching { store.syncPairedChildWithCloud() }
            delay(6000L)
        }
    }

    val isTakeoverLocked = uiState.isTakeoverLocked

    LaunchedEffect(isTakeoverLocked) {
        onTakeoverLockChanged(isTakeoverLocked)
    }

    BackHandler(enabled = isTakeoverLocked) {
        if (pinIntent == null) {
            pinIntent = PinIntent.UnlockChild
        } else {
            pinIntent = null
        }
    }

    fun handleRoleSelect(nextRole: RoleView) {
        if ((uiState.handOffActive || uiState.selectedChild.isPaired) &&
            uiState.roleView == RoleView.CHILD &&
            nextRole != RoleView.CHILD
        ) {
            pinIntent = PinIntent.ExitHandOff(nextRole)
        } else {
            store.setShowSetupWizard(false)
            store.setRoleView(nextRole)
        }
    }

    val isLearnerLoggedIn = uiState.roleView == RoleView.CHILD &&
        (uiState.selectedChild.isPaired || uiState.handOffActive)

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Surface(color = Color.White) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "SomaTime",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = SomaTeal
                        )
                        if (isLearnerLoggedIn) {
                            Text(
                                "Learner · ${uiState.selectedChild.name.split(" ").first()} · Grade ${uiState.selectedChild.grade}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SomaTeal
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(
                                    onClick = { store.setShowSetupWizard(true) }
                                ) {
                                    Icon(Icons.Rounded.Key, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Setup & Pair")
                                }
                                IconButton(
                                    onClick = { store.resetDemo() }
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Reset Demo")
                                }
                            }
                        }
                    }

                    if (!isLearnerLoggedIn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.roleView == RoleView.PARENT,
                                onClick = { handleRoleSelect(RoleView.PARENT) },
                                label = { Text("Parent") },
                                leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.roleView == RoleView.CHILD,
                                onClick = { handleRoleSelect(RoleView.CHILD) },
                                label = { Text("Child") },
                                leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = uiState.roleView == RoleView.TEACHER,
                                onClick = { handleRoleSelect(RoleView.TEACHER) },
                                label = { Text("Teacher") },
                                leadingIcon = { Icon(Icons.Rounded.School, contentDescription = null) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.showSetupWizard) {
                SetupWizardScreen(
                    uiState = uiState,
                    onRegisterParentAndChild = store::registerParentAndChildAsync,
                    onPairLearnerCode = store::pairLearnerByCodeAsync,
                    onRegisterTeacher = store::registerTeacherAsync,
                    onLoginTeacher = store::loginTeacherAsync,
                    onRetryCloudSync = store::retryCloudSyncNow,
                    onDismiss = { store.setShowSetupWizard(false) }
                )
            } else {
                when (uiState.roleView) {
                    RoleView.PARENT -> ParentDashboardScreen(
                        uiState = uiState,
                        onSelectChild = store::selectChild,
                        onUpdateChildCap = { id, cap ->
                            store.updateChild(id) { it.copy(dailyCapMins = cap) }
                        },
                        onUpdateExchangeRate = { id, rate ->
                            store.updateChild(id) { it.copy(exchangeRateMins = rate) }
                        },
                        onToggleMandatory = { id, area ->
                            store.updateChild(id) { c ->
                                val next = if (area in c.mandatorySubjects) {
                                    c.mandatorySubjects - area
                                } else {
                                    c.mandatorySubjects + area
                                }
                                c.copy(mandatorySubjects = next)
                            }
                        },
                        onSetApprovalRequired = { id, req ->
                            store.updateChild(id) { it.copy(requireParentApproval = req) }
                        },
                        onSetDeviceMode = { id, mode ->
                            store.updateChild(id) { it.copy(deviceMode = mode) }
                        },
                        onActivateHandOff = store::activateHandOffMode,
                        onAddChild = store::addChild,
                        onRegeneratePairingCode = store::regeneratePairingCode,
                        onRetryCloudSync = store::retryCloudSyncNow,
                        onOpenSetupWizard = { store.setShowSetupWizard(true) },
                        onRequestApprovalPin = { approval ->
                            pinIntent = PinIntent.ApproveMinutes(approval)
                        },
                        onDeclineApproval = { childId, approvalId ->
                            store.resolveApproval(childId, approvalId, approved = false)
                        }
                    )

                    RoleView.CHILD -> ChildModeScreen(
                        uiState = uiState,
                        onSimulateTimeExpired = store::simulateTimeExpired,
                        onOpenPinUnlock = { pinIntent = PinIntent.UnlockChild },
                        onOpenExitHandOff = { pinIntent = PinIntent.ExitHandOff(RoleView.PARENT) },
                        onOpenSetupWizard = { store.setShowSetupWizard(true, SetupWizardTab.LEARNER) },
                        onRecordDisplayedAssignment = store::recordDisplayedAssignment,
                        onMinimizeAndResumeApp = onMinimizeAndResumeApp,
                        onCompleteLesson = store::completeLesson
                    )

                    RoleView.TEACHER -> TeacherPortalScreen(
                        uiState = uiState,
                        onOpenTeacherAuth = { store.setShowSetupWizard(true, SetupWizardTab.TEACHER) },
                        onLogoutTeacher = store::logoutTeacher,
                        onPushAssignment = store::pushAssignment,
                        onPreviewChildMode = { store.setRoleView(RoleView.CHILD) }
                    )
                }
            }
        }
    }

    when (val intent = pinIntent) {
        is PinIntent.ExitHandOff -> PinDialog(
            title = "Exit Protected Child Mode",
            subtitle = "Enter Parent PIN (${uiState.parentPin}) to exit ${uiState.selectedChild.name}'s locked mode.",
            expectedPin = uiState.parentPin,
            confirmLabel = "Exit to Parent",
            onSuccess = {
                store.exitHandOffTo(intent.targetRole)
                pinIntent = null
            },
            onDismiss = { pinIntent = null }
        )

        PinIntent.UnlockChild -> PinDialog(
            title = "Parent PIN Screen Unlock",
            subtitle = "Enter Parent PIN (${uiState.parentPin}) to grant +15 mins to ${uiState.selectedChild.name}.",
            expectedPin = uiState.parentPin,
            confirmLabel = "Unlock +15m",
            onSuccess = {
                store.unlockWithParentPin(uiState.selectedChild.id, 15)
                pinIntent = null
            },
            onDismiss = { pinIntent = null }
        )

        is PinIntent.ApproveMinutes -> PinDialog(
            title = "Approve Earned Minutes",
            subtitle = "Confirm +${intent.approval.minutesEarned} mins for ${intent.approval.lessonTitle}.",
            expectedPin = uiState.parentPin,
            confirmLabel = "Approve",
            onSuccess = {
                store.resolveApproval(uiState.selectedChild.id, intent.approval.id, approved = true)
                pinIntent = null
            },
            onDismiss = { pinIntent = null }
        )

        null -> Unit
    }
}
