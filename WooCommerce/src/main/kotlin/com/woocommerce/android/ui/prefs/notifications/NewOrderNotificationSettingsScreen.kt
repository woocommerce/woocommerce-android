package com.woocommerce.android.ui.prefs.notifications

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BigDecimalTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.compose.SettingsSectionHeader
import com.woocommerce.android.ui.prefs.notifications.NewOrderNotificationSettingsViewModel.NotificationPreference
import com.woocommerce.android.ui.prefs.notifications.NewOrderNotificationSettingsViewModel.ViewState
import com.woocommerce.android.ui.prefs.notifications.compose.EnableNotificationsCard
import com.woocommerce.android.ui.prefs.notifications.compose.NotificationPreferenceOption
import java.math.BigDecimal

@Composable
fun NewOrderNotificationSettingsScreen(viewModel: NewOrderNotificationSettingsViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        NewOrderNotificationSettingsScreen(
            viewState = viewState,
            onNotificationsEnabledChanged = viewModel::onNotificationsEnabledChanged,
            onNotificationPreferenceChanged = viewModel::onNotificationPreferenceChanged
        )
    }
}

@Composable
fun NewOrderNotificationSettingsScreen(
    viewState: ViewState,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    onNotificationPreferenceChanged: (NotificationPreference) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            EnableNotificationsCard(
                title = stringResource(R.string.settings_notifs_new_orders_enable_title),
                description = stringResource(R.string.settings_notifs_new_orders_enable_description),
                isEnabled = viewState.notificationsEnabled,
                onEnabledChanged = onNotificationsEnabledChanged
            )
            SettingsSectionHeader(
                text = stringResource(R.string.settings_notifs_notify_me_for),
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp)
            )
            NotificationPreferenceOption(
                title = stringResource(R.string.settings_notifs_new_orders_all_title),
                description = stringResource(R.string.settings_notifs_new_orders_all_description),
                selected = viewState.notificationPreference == NotificationPreference.AllOrders,
                enabled = viewState.notificationsEnabled,
                onClick = { onNotificationPreferenceChanged(NotificationPreference.AllOrders) }
            )
            val highValuePreference = viewState.notificationPreference as? NotificationPreference.HighValueOrders
            NotificationPreferenceOption(
                title = stringResource(R.string.settings_notifs_new_orders_high_value_title),
                description = stringResource(R.string.settings_notifs_new_orders_high_value_description),
                selected = highValuePreference != null,
                enabled = viewState.notificationsEnabled,
                onClick = { onNotificationPreferenceChanged(NotificationPreference.HighValueOrders()) }
            )
            AnimatedVisibility(visible = highValuePreference != null) {
                highValuePreference?.let { preference ->
                    ThresholdAmountField(
                        amount = preference.thresholdAmount,
                        currencySymbol = viewState.currencySymbol,
                        enabled = viewState.notificationsEnabled,
                        onAmountChanged = { amount ->
                            onNotificationPreferenceChanged(preference.copy(thresholdAmount = amount))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThresholdAmountField(
    amount: BigDecimal,
    currencySymbol: String,
    enabled: Boolean,
    onAmountChanged: (BigDecimal) -> Unit
) {
    WCOutlinedTypedTextField(
        value = amount,
        onValueChange = onAmountChanged,
        label = stringResource(R.string.settings_notifs_new_orders_threshold, currencySymbol),
        valueMapper = BigDecimalTextFieldValueMapper.create(supportsNegativeValue = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, end = 16.dp),
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun NewOrderNotificationSettingsScreenPreview() {
    WooThemeWithBackground {
        NewOrderNotificationSettingsScreen(
            viewState = ViewState(
                notificationPreference = NotificationPreference.HighValueOrders(),
                currencySymbol = "$"
            ),
            onNotificationsEnabledChanged = {},
            onNotificationPreferenceChanged = {}
        )
    }
}
