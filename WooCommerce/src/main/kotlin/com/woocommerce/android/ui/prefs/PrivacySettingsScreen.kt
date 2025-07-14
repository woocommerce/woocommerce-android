package com.woocommerce.android.ui.prefs

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.icons.OpenInNew

@Composable
fun PrivacySettingsScreen(
    viewModel: PrivacySettingsViewModel,
) {
    val state: PrivacySettingsViewModel.State by viewModel.state.observeAsState(
        PrivacySettingsViewModel.State(
            sendUsageStats = false,
            crashReportingEnabled = false,
            progressBarVisible = false
        )
    )
    PrivacySettingsScreen(
        state,
        onAnalyticsSettingChanged = viewModel::onSendStatsSettingChanged,
        onReportCrashesChanged = viewModel::onCrashReportingSettingChanged,
        onAdvertisingOptionsClicked = viewModel::onWebOptionsClicked,
        onUsageTrackerClicked = viewModel::onUsageTrackerClicked,
        onPoliciesClicked = viewModel::onPoliciesClicked,
    )
}

@Composable
fun PrivacySettingsScreen(
    state: PrivacySettingsViewModel.State,
    onAnalyticsSettingChanged: (Boolean) -> Unit,
    onReportCrashesChanged: (Boolean) -> Unit,
    onAdvertisingOptionsClicked: () -> Unit,
    onUsageTrackerClicked: () -> Unit,
    onPoliciesClicked: () -> Unit,
) {
    Scaffold(backgroundColor = MaterialTheme.colors.surface) { paddingValues ->
        AnimatedVisibility(
            visible = state.progressBarVisible,
            enter = slideInVertically(),
            exit = slideOutVertically()
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                style = MaterialTheme.typography.h6,
                text = stringResource(R.string.settings_privacy_header),
                modifier = Modifier.padding(top = 16.dp, start = 16.dp)
            )
            Text(
                style = MaterialTheme.typography.body2,
                text = stringResource(R.string.settings_privacy_statement),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
            ) {
                Column {
                    OptionRowWithHeader(
                        sectionHeader = stringResource(R.string.settings_tracking_header),
                        sectionTitle = stringResource(R.string.settings_tracking_analytics),
                        sectionDescription = stringResource(R.string.settings_tracking_analytics_description),
                        onRowClicked = { onAnalyticsSettingChanged(!state.sendUsageStats) }
                    ) {
                        Switch(
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            checked = state.sendUsageStats,
                            onCheckedChange = onAnalyticsSettingChanged,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OptionRowWithHeader(
                        sectionHeader = stringResource(R.string.settings_more_privacy_options_header),
                        sectionTitle = stringResource(R.string.settings_web_options),
                        sectionDescription = stringResource(R.string.settings_web_options_description),
                        onRowClicked = onAdvertisingOptionsClicked
                    ) {
                        IconButton(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onClick = onAdvertisingOptionsClicked
                        ) {
                            Icon(
                                imageVector = OpenInNew,
                                contentDescription = stringResource(id = R.string.settings_web_options)
                            )
                        }
                    }
                    OptionRow(
                        onRowClicked = onUsageTrackerClicked,
                        sectionTitle = stringResource(R.string.settings_usage_tracking),
                        sectionDescription = stringResource(R.string.settings_usage_tracking_description),
                    ) {
                        IconButton(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            onClick = onUsageTrackerClicked,
                        ) {
                            Icon(
                                imageVector = OpenInNew,
                                contentDescription = stringResource(id = R.string.settings_usage_tracking)
                            )
                        }
                    }
                    OptionRow(
                        onRowClicked = onPoliciesClicked,
                        sectionTitle = stringResource(R.string.settings_privacy_cookies_polices),
                        sectionDescription = stringResource(R.string.settings_privacy_cookies_polices_description),
                        actionContent = null,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OptionRowWithHeader(
                        sectionHeader = stringResource(R.string.settings_reports_header),
                        sectionTitle = stringResource(R.string.settings_reports_report_crashes),
                        sectionDescription = stringResource(R.string.settings_reports_report_crashes_description),
                        onRowClicked = { onReportCrashesChanged(!state.crashReportingEnabled) }
                    ) {
                        Switch(
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colors.primary
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            checked = state.crashReportingEnabled,
                            onCheckedChange = onReportCrashesChanged,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionRowWithHeader(
    sectionHeader: String,
    sectionTitle: String,
    sectionDescription: String,
    modifier: Modifier = Modifier,
    onRowClicked: () -> Unit,
    actionContent: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = sectionHeader,
            style = MaterialTheme.typography.button,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colors.primary,
        )
        OptionRow(
            onRowClicked,
            sectionTitle,
            sectionDescription,
            actionContent = actionContent
        )
    }
}

@Composable
fun OptionRow(
    onRowClicked: () -> Unit,
    sectionTitle: String,
    sectionDescription: String,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)?,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clickable {
                onRowClicked()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = sectionTitle,
                style = MaterialTheme.typography.subtitle1,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                style = textAppearanceWooBody2(),
                text = sectionDescription,
            )
        }
        if (actionContent != null) {
            Divider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 8.dp)
                    .width(1.dp)
            )
            actionContent()
        }
    }
}

@Composable
// Style of TextAppearance.Woo.Body2
private fun textAppearanceWooBody2() = TextStyle(
    lineHeight = 20.sp,
    color = MaterialTheme.colors.onSurface.copy(
        alpha = 0.60f
    ),
    fontSize = 14.sp,
)

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Smaller screen", device = Devices.NEXUS_5)
@Composable
private fun Default() {
    WooThemeWithBackground {
        PrivacySettingsScreen(
            state = PrivacySettingsViewModel.State(
                sendUsageStats = true,
                crashReportingEnabled = false,
                progressBarVisible = true
            ),
            {},
            {},
            {},
            {},
            {}
        )
    }
}
