package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.approval.WooPosManagerApprovalAction
import com.woocommerce.android.ui.woopos.roles.approval.WooPosManagerApprovalDialog
import com.woocommerce.android.ui.woopos.roles.approval.WooPosManagerApprovalViewModel

@Composable
internal fun WooPosSettingsSecurityRoute(
    state: WooPosSettingsSecurityState,
    onShowPINEditor: (WooPosLocalRole, Boolean) -> Unit,
    viewModel: WooPosSettingsSecurityViewModel = hiltViewModel(),
    approval: WooPosManagerApprovalViewModel = hiltViewModel(key = "security_approval"),
    pinEditor: WooPosSettingsSecurityPINSetupViewModel = hiltViewModel(),
) {
    val request by approval.state.collectAsState()
    val openEditor: (WooPosLocalRole, Boolean) -> Unit = { role, changingPIN ->
        pinEditor.start(role, changingPIN)
        onShowPINEditor(role, changingPIN)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { approval.dismiss() }
    DisposableEffect(approval) { onDispose { approval.dismiss() } }
    WooPosSettingsSecurityScreen(
        state = state,
        onSetUpPIN = { openEditor(it, false) },
        onChangePIN = { role ->
            val action = viewModel.approvalForPINChange(role)
            if (action != null) {
                approval.requestFreshApproval(action) {
                    openEditor(role, true)
                    true
                }
            } else {
                openEditor(role, true)
            }
        },
        onDeleteCashierPIN = viewModel::deleteCashierPIN,
        onLockSettingsChanged = viewModel::updateLockSettings,
        onProtectionTurnedOff = {
            approval.requestFreshApproval(WooPosManagerApprovalAction.DisablePINProtection, viewModel::removeProtection)
        },
    )
    request?.let { WooPosManagerApprovalDialog(it, approval::onUIEvent) }
}
