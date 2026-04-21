package com.woocommerce.android.ui.prefs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun PrivacySettingsPolicesScreen(
    onPrivacyPolicyClicked: () -> Unit,
    onCookiePolicyClicked: () -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            OptionRow(
                sectionTitle = stringResource(R.string.settings_policies_privacy_policy),
                sectionDescription = stringResource(R.string.settings_policies_privacy_policy_description),
                onRowClicked = onPrivacyPolicyClicked
            ) {
                IconButton(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = onPrivacyPolicyClicked
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                        contentDescription = stringResource(id = R.string.settings_privacy_policy)
                    )
                }
            }
            OptionRow(
                sectionTitle = stringResource(R.string.settings_policies_cookie_policy),
                sectionDescription = stringResource(R.string.settings_policies_cookie_policy_description),
                onRowClicked = onCookiePolicyClicked
            ) {
                IconButton(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    onClick = onCookiePolicyClicked
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new_24dp),
                        contentDescription = stringResource(id = R.string.settings_privacy_cookies_polices)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PrivacySettingsPolicesScreenPreview() {
    PrivacySettingsPolicesScreen(
        onPrivacyPolicyClicked = {},
        onCookiePolicyClicked = {}
    )
}
