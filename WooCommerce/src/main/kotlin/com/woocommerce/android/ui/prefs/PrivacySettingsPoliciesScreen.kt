package com.woocommerce.android.ui.prefs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.icons.OpenInNew

@Composable
fun PrivacySettingsPolicesScreen() {
    Scaffold(backgroundColor = MaterialTheme.colors.surface) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            OptionRow(
                sectionTitle = stringResource(R.string.settings_policies_privacy_policy),
                sectionDescription = stringResource(R.string.settings_policies_privacy_policy_description),
                onRowClicked = { /*TODO*/ },
                actionContent = {
                    IconButton(modifier = Modifier.padding(horizontal = 8.dp), onClick = { /*TODO*/
                    }) {
                        Icon(
                            imageVector = OpenInNew,
                            contentDescription = stringResource(id = R.string.settings_privacy_policy)
                        )
                    }
                }
            )
            OptionRow(
                sectionTitle = stringResource(R.string.settings_policies_cookie_policy),
                sectionDescription = stringResource(R.string.settings_policies_cookie_policy_description),
                onRowClicked = { /*TODO*/ },
                actionContent = {
                    IconButton(modifier = Modifier.padding(horizontal = 8.dp), onClick = { /*TODO*/
                    }) {
                        Icon(
                            imageVector = OpenInNew,
                            contentDescription = stringResource(id = R.string.settings_privacy_cookies_polices)
                        )
                    }
                }
            )
        }
    }
}
