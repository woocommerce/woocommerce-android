package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.lock.WooPosLockError
import com.woocommerce.android.ui.woopos.lock.WooPosLockScreen
import com.woocommerce.android.ui.woopos.lock.WooPosLockScreenState

@Composable
fun WooPosSettingsSecurityScreen(viewModel: WooPosSettingsSecurityViewModel = hiltViewModel()) {
    if (!viewModel.isEnabled) return
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value),
    ) {
        WooPosText(stringResource(R.string.woopos_security_setup), style = WooPosTypography.Heading)
        WooPosText(stringResource(R.string.woopos_security_description), style = WooPosTypography.BodyLarge)
        WooPosText(stringResource(R.string.woopos_security_mock_notice), style = WooPosTypography.BodyMedium)
        WooPosButton(text = stringResource(R.string.woopos_security_get_started), onClick = { viewModel.showSetup() })
        PreviewMenu(
            title = stringResource(R.string.woopos_security_setup_states),
            options = listOf(
                R.string.woopos_security_choose_pin to { viewModel.showSetup() },
                R.string.woopos_security_confirm_pin to {
                    viewModel.showSetup(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.CONFIRM))
                },
                R.string.woopos_security_mismatch to { viewModel.showSetup(WooPosPinSetupState(mismatch = true)) },
                R.string.woopos_security_saving to {
                    viewModel.showSetup(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.CHECKING))
                },
                R.string.woopos_security_success to {
                    viewModel.showSetup(WooPosPinSetupState(stage = WooPosPinSetupState.Stage.SUCCESS))
                },
            ),
        )
        PreviewMenu(
            title = stringResource(R.string.woopos_security_lock_states),
            options = listOf(
                R.string.woopos_lock_enter_pin to { viewModel.showLock(WooPosLockScreenState()) },
                R.string.woopos_security_partial_entry to {
                    viewModel.showLock(WooPosLockScreenState(enteredDigits = 2))
                },
                R.string.woopos_lock_incorrect to {
                    viewModel.showLock(WooPosLockScreenState(error = WooPosLockError.INCORRECT))
                },
                R.string.woopos_lock_checking to {
                    viewModel.showLock(WooPosLockScreenState(enteredDigits = 4, isChecking = true))
                },
                R.string.woopos_security_countdown to {
                    viewModel.showLock(WooPosLockScreenState(remainingSeconds = 30))
                },
                R.string.woopos_lock_storage_error to {
                    viewModel.showLock(WooPosLockScreenState(error = WooPosLockError.STORAGE))
                },
                R.string.woopos_lock_recovery_pending to {
                    viewModel.showLock(WooPosLockScreenState(error = WooPosLockError.RECOVERY_UNAVAILABLE))
                },
            ),
        )
    }
}

@Composable
fun WooPosSettingsSecurityDialogs(viewModel: WooPosSettingsSecurityViewModel = hiltViewModel()) {
    if (!viewModel.isEnabled) return
    val state by viewModel.state.collectAsState()
    val setupState by viewModel.setupState.collectAsState()
    WooPosPinSetupDialog(
        state = setupState,
        isVisible = state.showSetup,
        onUIEvent = viewModel::onSetupEvent,
        onClose = viewModel::close,
    )
    state.lock?.let { lock ->
        Dialog(onDismissRequest = viewModel::close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth()) {
                    WooPosText(
                        stringResource(R.string.woopos_security_mock_notice),
                        style = WooPosTypography.BodyMedium,
                        modifier = Modifier.weight(1f).padding(WooPosSpacing.Small.value),
                    )
                    TextButton(onClick = viewModel::close) {
                        WooPosText(stringResource(R.string.close), style = WooPosTypography.BodyLarge)
                    }
                }
                WooPosLockScreen(lock, viewModel::onLockEvent, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PreviewMenu(title: String, options: List<Pair<Int, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        WooPosOutlinedButton(text = title, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { WooPosText(stringResource(label), style = WooPosTypography.BodyMedium) },
                    onClick = {
                        expanded = false
                        action()
                    },
                )
            }
        }
    }
}
