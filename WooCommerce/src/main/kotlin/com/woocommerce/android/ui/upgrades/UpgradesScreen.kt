package com.woocommerce.android.ui.upgrades

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.CurrentPlanInfo.NonUpgradeable
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.CurrentPlanInfo.Upgradeable

@Composable
fun UpgradesScreen(viewModel: UpgradesViewModel) {
    val upgradesState by viewModel.upgradesState.observeAsState(UpgradesViewState())
    UpgradesScreen(
        state = upgradesState,
        onSubscribeNowClicked = viewModel::onSubscribeNowClicked,
        onReportSubscriptionIssueClicked = viewModel::onReportSubscriptionIssueClicked,
    )
}

@Composable
fun UpgradesScreen(
    state: UpgradesViewState,
    onSubscribeNowClicked: () -> Unit,
    onReportSubscriptionIssueClicked: (Context) -> Unit
) {
    Scaffold() { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.upgrades_subscription_status),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        modifier = Modifier.padding(vertical = 8.dp),
                        text = stringResource(R.string.upgrades_current_plan, state.currentPlan.name),
                    )
                    if (state.currentPlan is Upgradeable) {
                        Divider()
                        Button(
                            onClick = onSubscribeNowClicked,
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.upgrades_subscribe_now))
                        }
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val context = LocalContext.current
                    Text(
                        text = stringResource(R.string.upgrades_troubleshooting),
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = { onReportSubscriptionIssueClicked(context) },
                    ) {
                        Text(stringResource(R.string.upgrades_report_subscription_issue))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun UpgradesScreenPreviewShowSubscribeNow() {
    WooThemeWithBackground {
        UpgradesScreen(state = UpgradesViewState(Upgradeable("Free Trial")), {}, {})
    }
}

@Preview
@Composable
private fun UpgradesScreenPreviewHideSubscribeNow() {
    WooThemeWithBackground {
        UpgradesScreen(
            state = UpgradesViewState(NonUpgradeable("eCommerce")), {}, {}
        )
    }
}
