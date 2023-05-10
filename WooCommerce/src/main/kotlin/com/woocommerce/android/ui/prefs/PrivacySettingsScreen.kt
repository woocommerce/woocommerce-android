package com.woocommerce.android.ui.prefs

import android.content.res.Configuration
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
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
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
        PrivacySettingsViewModel.State(sendUsageStats = false, crashReportingEnabled = false)
    )
    PrivacySettingsScreen(
        state,
        onAnalyticsSettingChanged = viewModel::onSendStatsSettingChanged,
        onReportCrashesChanged = viewModel::onCrashReportingSettingChanged,
        onAdvertisingOptionsClicked = viewModel::onAdvertisingOptionsClicked,
        onPrivacyPolicyClicked = viewModel::onPrivacyPolicyClicked,
        onCookiePolicyClicked = viewModel::onCookiePolicyClicked,
    )
}

@Composable
fun PrivacySettingsScreen(
    state: PrivacySettingsViewModel.State,
    onAnalyticsSettingChanged: (Boolean) -> Unit,
    onReportCrashesChanged: (Boolean) -> Unit,
    onAdvertisingOptionsClicked: () -> Unit,
    onPrivacyPolicyClicked: () -> Unit,
    onCookiePolicyClicked: () -> Unit,
) {
    Scaffold(backgroundColor = MaterialTheme.colors.surface) { paddingValues ->
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
            Column(modifier = Modifier.fillMaxWidth()) {
                Column {
                    OptionRow(
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
                    OptionRow(
                        sectionHeader = stringResource(R.string.settings_more_privacy_options_header),
                        sectionTitle = stringResource(R.string.settings_advertising_options),
                        sectionDescription = stringResource(R.string.settings_advertising_options_description),
                        onRowClicked = onAdvertisingOptionsClicked
                    ) {
                        IconButton(
                            modifier = Modifier.padding(start = 8.dp),
                            onClick = onAdvertisingOptionsClicked
                        ) {
                            Icon(
                                imageVector = OpenInNew,
                                contentDescription = stringResource(id = R.string.settings_advertising_options)
                            )
                        }
                    }
                    ExplanationText(
                        onPrivacyPolicyClicked = onPrivacyPolicyClicked,
                        onCookiePolicyClicked = onCookiePolicyClicked
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OptionRow(
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

private const val COOKIES_TAG = "cookies"
private const val PRIVACY_TAG = "privacy"

@Composable
private fun ExplanationText(
    onPrivacyPolicyClicked: () -> Unit,
    onCookiePolicyClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val privacyText =
        stringResource(R.string.settings_advertising_options_explanation_privacy_policy)
    val cookieText = stringResource(R.string.settings_advertising_options_explanation_cookie_policy)
    val explanation = stringResource(
        R.string.settings_advertising_options_explanation, privacyText, cookieText
    )
    val annotatedString = AnnotatedString.Builder(explanation).apply {
        val defaultSpanStyle = SpanStyle(color = MaterialTheme.colors.onBackground)
        val linkSpanStyle = SpanStyle(
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colors.primary
        )

        addStyle(defaultSpanStyle, 0, explanation.length)

        val privacyPolicyRange =
            explanation.indexOf(privacyText)..explanation.indexOf(privacyText) + privacyText.length
        val cookiePolicyRange =
            explanation.indexOf(cookieText)..explanation.indexOf(cookieText) + cookieText.length

        addStyle(linkSpanStyle, privacyPolicyRange.first, privacyPolicyRange.last)
        addStyle(linkSpanStyle, cookiePolicyRange.first, cookiePolicyRange.last)

        addStringAnnotation(
            PRIVACY_TAG,
            explanation,
            privacyPolicyRange.first,
            privacyPolicyRange.last
        )
        addStringAnnotation(
            COOKIES_TAG,
            explanation,
            cookiePolicyRange.first,
            cookiePolicyRange.last
        )
    }.toAnnotatedString()

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations(PRIVACY_TAG, offset, offset).firstOrNull()
                ?.let { onPrivacyPolicyClicked.invoke() }
            annotatedString.getStringAnnotations(COOKIES_TAG, offset, offset).firstOrNull()
                ?.let { onCookiePolicyClicked.invoke() }
        },
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        style = MaterialTheme.typography.caption,
    )
}

@Composable
private fun OptionRow(
    sectionHeader: String,
    sectionTitle: String,
    sectionDescription: String,
    modifier: Modifier = Modifier,
    onRowClicked: () -> Unit,
    actionContent: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = sectionHeader,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colors.primary,
        )
        Row(
            modifier = Modifier
                .height(IntrinsicSize.Min)
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
                    style = textAppearanceWooBody2(),
                    text = sectionDescription,
                )
            }
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
@Composable
private fun Default() {
    WooThemeWithBackground {
        PrivacySettingsScreen(
            state = PrivacySettingsViewModel.State(
                sendUsageStats = true, crashReportingEnabled = false
            ),
            {}, {}, {}, {}, {}
        )
    }
}
