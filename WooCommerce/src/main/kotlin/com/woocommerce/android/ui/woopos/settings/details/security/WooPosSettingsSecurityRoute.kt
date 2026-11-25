package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalAction
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalDialog
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalViewModel

@Composable
internal fun WooPosSettingsSecurityRoute(
    state: WooPosSettingsSecurityState,
    onShowPINEditor: (WooPosLocalRole, Boolean) -> Unit,
    viewModel: WooPosSettingsSecurityViewModel = hiltViewModel(),
    approval: WooPosApprovalViewModel = hiltViewModel(key = "security_approval"),
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
        onProtectionTurnedOff = {
            approval.requestFreshApproval(WooPosApprovalAction.DisablePINProtection, viewModel::removeProtection)
        },
    )
    WooPosApprovalDialog(request, approval::onUIEvent)
}
