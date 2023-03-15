package com.woocommerce.android.ui.upgrades

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.*

@Composable
fun UpgradesScreen(viewModel: UpgradesViewModel) {
    val upgradesState by viewModel.upgradesState.observeAsState(Loading)
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
    onReportSubscriptionIssueClicked: () -> Unit
) {
    Scaffold { paddingValues ->
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
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    if (state is HasPlan) {
                        Text(
                            modifier = Modifier.padding(vertical = 8.dp),
                            text = stringResource(R.string.upgrades_current_plan, state.name),
                        )
                    }
                    if (state is Upgradeable || state is TrialEnded) {
                        Divider()
                        WCOutlinedButton(
                            onClick = onSubscribeNowClicked,
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.upgrades_subscribe_now))
                        }
                    }
                    Text(
                        style = MaterialTheme.typography.caption,
                        text = when (state) {
                            Loading -> ""
                            is NonUpgradeable -> stringResource(
                                R.string.upgrades_non_upgradeable_caption,
                                state.name,
                                state.currentPlanEndDate
                            )

                            is TrialEnded -> stringResource(R.string.upgrades_trial_ended_caption, state.planToUpgrade)
                            is Upgradeable -> stringResource(
                                R.string.upgrades_upgradeable_caption,
                                state.daysLeftInCurrentPlan,
                                state.currentPlanEndDate,
                                state.nextPlanMonthlyFee
                            )
                        }
                    )
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.upgrades_troubleshooting),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    WCOutlinedButton(
                        onClick = onReportSubscriptionIssueClicked
                    ) {
                        Text(stringResource(R.string.upgrades_report_subscription_issue))
                    }
                }
            }
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Upgradeable() {
    WooThemeWithBackground {
        UpgradesScreen(state =
        Upgradeable("Free Trial", 14, "March 2, 2023", "$45"), {}, {}
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrialEnded() {
    WooThemeWithBackground {
        UpgradesScreen(
            state = TrialEnded("Free Trial"), {}, {}
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NonUpgradeable() {
    WooThemeWithBackground {
        UpgradesScreen(state =
        NonUpgradeable("eCommerce", "March 2, 2023"), {}, {}
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Loading() {
    WooThemeWithBackground {
        UpgradesScreen(state = Loading, {}, {})
    }
}
