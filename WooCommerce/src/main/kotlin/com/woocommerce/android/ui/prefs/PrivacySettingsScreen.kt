package com.woocommerce.android.ui.prefs

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun PrivacySettingsScreen(
    viewModel: PrivacySettingsViewModel
) {
    val state: PrivacySettingsViewModel.State by viewModel.state.observeAsState(
        PrivacySettingsViewModel.State(sendUsageStats = false, crashReportingEnabled = false)
    )
    PrivacySettingsScreen(
        state,
        onAnalyticsSettingChanged = viewModel::onSendStatsSettingChanged,
    )
}

@Composable
fun PrivacySettingsScreen(
    state: PrivacySettingsViewModel.State,
    onAnalyticsSettingChanged: (Boolean) -> Unit,
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                style = MaterialTheme.typography.caption,
                text = stringResource(R.string.settings_privacy_statement),
                modifier = Modifier.padding(16.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                elevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_tracking_header),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(bottom = 16.dp),
                        color = MaterialTheme.colors.primary,
                    )
                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.settings_tracking_analytics),
                            )
                            Text(
                                style = MaterialTheme.typography.caption,
                                text = stringResource(R.string.settings_tracking_analytics_description),
                            )
                        }
                        Divider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                                .width(1.dp)
                        )
                        Switch(
                            modifier = Modifier.padding(start = 8.dp),
                            checked = state.sendUsageStats,
                            onCheckedChange = onAnalyticsSettingChanged,
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Default() {
    WooThemeWithBackground {
        PrivacySettingsScreen(
            state = PrivacySettingsViewModel.State(
                sendUsageStats = true,
                crashReportingEnabled = false
            ),
            onAnalyticsSettingChanged = {},
        )
    }
}
