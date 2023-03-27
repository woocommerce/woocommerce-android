package com.woocommerce.android.ui.upgrades

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.Error
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.HasPlan
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.Loading
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.NonUpgradeable
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.PlanEnded
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.TrialEnded
import com.woocommerce.android.ui.upgrades.UpgradesViewModel.UpgradesViewState.TrialInProgress
import java.time.Period

@Composable
fun UpgradesScreen(viewModel: UpgradesViewModel) {
    val upgradesState by viewModel.upgradesState.observeAsState(Loading)
    UpgradesScreen(
        state = upgradesState,
        onUpgradeNowClicked = viewModel::onSubscribeNowClicked,
        onReportSubscriptionIssueClicked = viewModel::onReportSubscriptionIssueClicked,
    )
}

@Composable
fun UpgradesScreen(
    state: UpgradesViewState,
    onUpgradeNowClicked: () -> Unit,
    onReportSubscriptionIssueClicked: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
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
                    if (state is TrialInProgress || state is TrialEnded) {
                        Divider()
                        WCOutlinedButton(
                            onClick = onUpgradeNowClicked,
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            Text(stringResource(R.string.upgrades_upgrade_now))
                        }
                    }

                    if (state is Loading) {
                        SkeletonView(width = 132.dp, height = 24.dp)
                    } else if (state is Error) {
                        Row {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null
                            )
                            Text(
                                stringResource(R.string.upgrades_error_fetching_data),
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    } else {
                        Text(
                            style = MaterialTheme.typography.caption,
                            text = when (state) {
                                Loading, Error -> ""
                                is PlanEnded -> stringResource(
                                    R.string.upgrades_current_plan_ended_caption,
                                    state.name
                                )
                                is NonUpgradeable -> stringResource(
                                    R.string.upgrades_non_upgradeable_caption,
                                    state.name,
                                    state.currentPlanEndDate
                                )

                                is TrialEnded -> stringResource(
                                    R.string.upgrades_trial_ended_caption,
                                    state.planToUpgrade
                                )

                                is TrialInProgress -> stringResource(
                                    R.string.upgrades_upgradeable_caption,
                                    state.freeTrialDuration.days,
                                    state.daysLeftInFreeTrial,
                                )
                            }
                        )
                    }
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
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrialInProgress() {
    WooThemeWithBackground {
        UpgradesScreen(
            state =
            TrialInProgress("Free Trial", Period.ofDays(14), "6 days"),
            {}, {}
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrialEnded() {
    WooThemeWithBackground {
        UpgradesScreen(
            state = TrialEnded("Trial ended"), {}, {}
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NonUpgradeable() {
    WooThemeWithBackground {
        UpgradesScreen(
            state =
            NonUpgradeable("eCommerce", "March 2, 2023"),
            {}, {}
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PlanEnded() {
    WooThemeWithBackground {
        UpgradesScreen(state = PlanEnded("eCommerce ended"), {}, {})
    }
}

@Preview(name = "Light mode")
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Loading() {
    WooThemeWithBackground {
        UpgradesScreen(state = Loading, {}, {})
    }
}

@Preview(name = "Light mode")
@Preview(name = "RTL mode", locale = "ar")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Error() {
    WooThemeWithBackground {
        UpgradesScreen(state = Error, {}, {})
    }
}
