package com.woocommerce.android.ui.prefs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.component.WooBodyText
import com.woocommerce.android.ui.designsystem.compose.component.WooLinearProgressIndicator
import com.woocommerce.android.ui.designsystem.compose.component.WooPageTitle
import com.woocommerce.android.ui.designsystem.compose.component.WooSectionHeader
import com.woocommerce.android.ui.designsystem.compose.component.WooSettingsRow
import com.woocommerce.android.ui.designsystem.compose.component.WooSwitchSettingsRow
import com.woocommerce.android.ui.designsystem.compose.component.WooTopAppBar
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemThemeWithBackground

@Composable
fun PrivacySettingsScreen(
    viewModel: PrivacySettingsViewModel,
    onBackClick: () -> Unit,
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
        onBackClick = onBackClick,
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
    onBackClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            WooTopAppBar(
                title = stringResource(R.string.privacy_settings),
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                navigationIconContentDescription = stringResource(R.string.back),
                onNavigationClick = onBackClick,
                windowInsets = WindowInsets(0),
            )
        },
        containerColor = WooTheme.colors.surface.default,
    ) { paddingValues ->
        AnimatedVisibility(
            visible = state.progressBarVisible,
            enter = slideInVertically(),
            exit = slideOutVertically(),
            modifier = Modifier.padding(paddingValues),
        ) {
            WooLinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            WooPageTitle(
                text = stringResource(R.string.settings_privacy_header),
                modifier = Modifier.padding(
                    top = WooTheme.padding.padding5,
                    start = WooTheme.padding.padding5,
                    end = WooTheme.padding.padding5,
                )
            )
            WooBodyText(
                text = stringResource(R.string.settings_privacy_statement),
                modifier = Modifier.padding(
                    start = WooTheme.padding.padding5,
                    end = WooTheme.padding.padding5,
                    top = WooTheme.spacing.space3,
                )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = WooTheme.spacing.space8)
            ) {
                Column {
                    PrivacySettingsSectionHeader(
                        text = stringResource(R.string.settings_tracking_header),
                    )
                    WooSwitchSettingsRow(
                        title = stringResource(R.string.settings_tracking_analytics),
                        description = stringResource(R.string.settings_tracking_analytics_description),
                        checked = state.sendUsageStats,
                        onCheckedChange = onAnalyticsSettingChanged,
                    )
                }
                Spacer(modifier = Modifier.height(WooTheme.spacing.space5))
                Column {
                    PrivacySettingsSectionHeader(
                        text = stringResource(R.string.settings_more_privacy_options_header),
                    )
                    ExternalLinkSettingsRow(
                        title = stringResource(R.string.settings_web_options),
                        description = stringResource(R.string.settings_web_options_description),
                        onClick = onAdvertisingOptionsClicked,
                    )
                    ExternalLinkSettingsRow(
                        title = stringResource(R.string.settings_usage_tracking),
                        description = stringResource(R.string.settings_usage_tracking_description),
                        onClick = onUsageTrackerClicked,
                    )
                    WooSettingsRow(
                        title = stringResource(R.string.settings_privacy_cookies_polices),
                        description = stringResource(R.string.settings_privacy_cookies_polices_description),
                        onClick = onPoliciesClicked,
                    )
                }
                Spacer(modifier = Modifier.height(WooTheme.spacing.space7))
                Column {
                    PrivacySettingsSectionHeader(
                        text = stringResource(R.string.settings_reports_header),
                    )
                    WooSwitchSettingsRow(
                        title = stringResource(R.string.settings_reports_report_crashes),
                        description = stringResource(R.string.settings_reports_report_crashes_description),
                        checked = state.crashReportingEnabled,
                        onCheckedChange = onReportCrashesChanged,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacySettingsSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    WooSectionHeader(
        text = text,
        modifier = modifier.padding(horizontal = WooTheme.padding.padding5)
    )
}

@Composable
private fun ExternalLinkSettingsRow(
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooSettingsRow(
        title = title,
        description = description,
        onClick = onClick,
        modifier = modifier,
        trailingContent = {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                contentDescription = null,
            )
        },
    )
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
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                style = textAppearanceWooBody2(),
                text = sectionDescription,
            )
        }
        if (actionContent != null) {
            HorizontalDivider(
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
    color = MaterialTheme.colorScheme.onSurface.copy(
        alpha = 0.60f
    ),
    fontSize = 14.sp,
)

@PreviewLightDark
@Composable
private fun PrivacySettingsScreenPreview() {
    PrivacySettingsScreenPreviewContent()
}

@Preview(name = "RTL mode", locale = "ar")
@Composable
private fun PrivacySettingsScreenRtlPreview() {
    PrivacySettingsScreenPreviewContent()
}

@Preview(name = "Smaller screen", device = Devices.NEXUS_5)
@Composable
private fun PrivacySettingsScreenSmallPreview() {
    PrivacySettingsScreenPreviewContent()
}

@Preview(name = "Large font", fontScale = 1.5f)
@Composable
private fun PrivacySettingsScreenLargeFontPreview() {
    PrivacySettingsScreenPreviewContent()
}

@Preview(name = "Progress visible")
@Composable
private fun PrivacySettingsScreenProgressPreview() {
    PrivacySettingsScreenPreviewContent(
        progressBarVisible = true,
    )
}

@Composable
private fun PrivacySettingsScreenPreviewContent(
    progressBarVisible: Boolean = false,
) {
    WooDesignSystemThemeWithBackground {
        PrivacySettingsScreen(
            state = PrivacySettingsViewModel.State(
                sendUsageStats = true,
                crashReportingEnabled = false,
                progressBarVisible = progressBarVisible
            ),
            onAnalyticsSettingChanged = {},
            onReportCrashesChanged = {},
            onAdvertisingOptionsClicked = {},
            onUsageTrackerClicked = {},
            onPoliciesClicked = {},
            onBackClick = {},
        )
    }
}
