package com.woocommerce.android.ui.prefs.notifications

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.prefs.compose.SettingsSectionHeader
import com.woocommerce.android.ui.prefs.notifications.NewOrderNotificationSettingsViewModel.NotificationPreference
import com.woocommerce.android.ui.prefs.notifications.NewOrderNotificationSettingsViewModel.ViewState
import com.woocommerce.android.ui.prefs.notifications.compose.EnableNotificationsCard
import com.woocommerce.android.ui.prefs.notifications.compose.NotificationPreferenceOption
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.LEFT_SPACE
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.RIGHT
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.RIGHT_SPACE
import org.wordpress.android.fluxc.utils.WCCurrencyUtils
import java.math.BigDecimal
import java.util.Locale

@Composable
fun NewOrderNotificationSettingsScreen(viewModel: NewOrderNotificationSettingsViewModel) {
    val viewState by viewModel.viewState.collectAsState()
    NewOrderNotificationSettingsScreen(
        viewState = viewState,
        onNotificationsEnabledChanged = viewModel::onNotificationsEnabledChanged,
        onNotificationPreferenceChanged = viewModel::onNotificationPreferenceChanged
    )
}

@Composable
fun NewOrderNotificationSettingsScreen(
    viewState: ViewState,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    onNotificationPreferenceChanged: (NotificationPreference) -> Unit
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { paddingValues ->
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
                        currencyPosition = viewState.currencyPosition,
                        decimalSeparator = viewState.currencyDecimalSeparator,
                        thousandSeparator = viewState.currencyThousandSeparator,
                        numberOfDecimals = viewState.currencyDecimalNumber,
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
    currencyPosition: CurrencyPosition,
    decimalSeparator: String,
    thousandSeparator: String,
    numberOfDecimals: Int,
    enabled: Boolean,
    onAmountChanged: (BigDecimal) -> Unit
) {
    var textFieldValue by rememberSaveable(
        amount.toPlainString(),
        decimalSeparator,
        thousandSeparator,
        numberOfDecimals,
        stateSaver = TextFieldValue.Saver
    ) {
        val formattedAmount = WCCurrencyUtils.formatCurrencyForDisplay(
            rawValue = amount.toDouble(),
            currencyDecimalNumber = numberOfDecimals,
            currencyDecimalSeparator = decimalSeparator,
            currencyThousandSeparator = thousandSeparator,
            locale = Locale.ROOT
        )
        mutableStateOf(
            TextFieldValue(
                text = formattedAmount,
                selection = TextRange(formattedAmount.length)
            )
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currencyPosition == LEFT || currencyPosition == LEFT_SPACE) {
            CurrencySymbolText(currencySymbol = currencySymbol, enabled = enabled)
        }

        BasicTextField(
            value = textFieldValue,
            onValueChange = { updatedValue ->
                if (updatedValue.text == textFieldValue.text) {
                    // Value is unchanged; skip formatting.
                    textFieldValue = updatedValue
                } else {
                    val updatedAmount = WCCurrencyUtils.cleanFullFormattedCurrencyInput(
                        text = updatedValue.text,
                        decimals = numberOfDecimals
                    )
                        ?: BigDecimal.ZERO
                    val formattedAmount = WCCurrencyUtils.formatCurrencyForDisplay(
                        rawValue = updatedAmount.toDouble(),
                        currencyDecimalNumber = numberOfDecimals,
                        currencyDecimalSeparator = decimalSeparator,
                        currencyThousandSeparator = thousandSeparator,
                        locale = Locale.ROOT
                    )
                    textFieldValue = updatedValue.copy(
                        text = formattedAmount,
                        selection = TextRange(
                            (updatedValue.selection.start + formattedAmount.length - updatedValue.text.length).coerceIn(
                                0,
                                formattedAmount.length
                            )
                        )
                    )
                    if (updatedAmount.compareTo(amount) != 0) {
                        onAmountChanged(updatedAmount)
                    }
                }
            },
            modifier = Modifier.weight(1f),
            enabled = enabled,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.let {
                    if (!enabled) it.copy(alpha = 0.38f) else it
                },
                fontSize = 56.sp
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface)
        )

        if (currencyPosition == RIGHT || currencyPosition == RIGHT_SPACE) {
            CurrencySymbolText(currencySymbol = currencySymbol, enabled = enabled)
        }
    }
}

@Composable
private fun CurrencySymbolText(
    currencySymbol: String,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Text(
        text = currencySymbol,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant.let {
                if (!enabled) it.copy(alpha = 0.38f) else it
            },
            fontSize = 56.sp
        )
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
                currencySymbol = "$",
                currencyPosition = LEFT,
                currencyDecimalSeparator = ".",
                currencyThousandSeparator = ",",
                currencyDecimalNumber = 2
            ),
            onNotificationsEnabledChanged = {},
            onNotificationPreferenceChanged = {}
        )
    }
}
