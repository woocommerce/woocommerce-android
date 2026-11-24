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
) {
    val request by approval.state.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { approval.dismiss() }
    DisposableEffect(approval) { onDispose { approval.dismiss() } }
    WooPosSettingsSecurityScreen(
        state = state,
        onSetUpPIN = { onShowPINEditor(it, false) },
        onChangePIN = { role ->
            if (role == WooPosLocalRole.ADMIN) {
                approval.requestFreshApproval(WooPosManagerApprovalAction.ChangeAdminPIN) {
                    onShowPINEditor(role, true)
                    true
                }
            } else {
                onShowPINEditor(role, true)
            }
        },
        onDeleteCashierPIN = viewModel::deleteCashierPIN,
        onProtectionTurnedOff = {
            approval.requestFreshApproval(WooPosManagerApprovalAction.DisablePINProtection, viewModel::removeProtection)
        },
    )
    request?.let { WooPosManagerApprovalDialog(it, approval::onUIEvent) }
}
