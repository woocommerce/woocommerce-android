package com.woocommerce.android.ui.blaze.creation.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheetLayout
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import kotlinx.coroutines.launch

@Composable
fun CampaignBudgetScreen(viewModel: BlazeCampaignBudgetViewModel) {
    CampaignBudgetScreen(
        onBackPressed = viewModel::onBackPressed
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CampaignBudgetScreen(
    onBackPressed: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded }
    )
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_budget_toolbar_title),
                onNavigationButtonClick = onBackPressed,
                navigationIcon = Filled.ArrowBack
            )
        },
        modifier = Modifier.background(MaterialTheme.colors.surface)
    ) { paddingValues ->
        WCModalBottomSheetLayout(
            sheetState = modalSheetState,
            sheetContent = {
                DurationSheetContent(
                    durationInDays = 0,
                    onApplyTapped = {
                        coroutineScope.launch { modalSheetState.hide() }
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .background(MaterialTheme.colors.surface)
            ) {

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(id = R.string.blaze_campaign_budget_subtitle),
                        style = MaterialTheme.typography.subtitle1,
                        textAlign = TextAlign.Center,
                        color = colorResource(id = R.color.color_on_surface_medium)
                    )
                    Spacer(modifier = Modifier.height(94.dp))
                    Text(
                        modifier = Modifier.padding(bottom = 8.dp),
                        text = stringResource(id = R.string.blaze_campaign_budget_total_spend),
                        style = MaterialTheme.typography.body1,
                        color = colorResource(id = R.color.color_on_surface_medium)
                    )
                    Text(
                        text = "$35 USD",
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(id = R.string.blaze_campaign_budget_days_duration),
                        style = MaterialTheme.typography.h4,
                        color = colorResource(id = R.color.color_on_surface_medium)
                    )
                    Text(
                        modifier = Modifier.padding(top = 40.dp),
                        text = stringResource(id = R.string.blaze_campaign_budget_daily_spend),
                        color = colorResource(id = R.color.color_on_surface_medium)
                    )
                    Slider(
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                        value = 35f,
                        valueRange = 35f..350f,
                        onValueChange = { /* TODO */ }
                    )
                    Row {
                        Text(text = stringResource(id = R.string.blaze_campaign_budget_reach_forecast))
                        Icon(
                            modifier = Modifier.padding(start = 4.dp),
                            painter = painterResource(id = R.drawable.ic_info_outline_20dp),
                            contentDescription = null
                        )
                    }
                    Text(text = "2400-3000")
                    Spacer(modifier = Modifier.height(24.dp))
                }
                // Duration content
                Column() {
                    Divider()

                }

            }
        }
    }
}

@Composable
private fun DurationSheetContent(
    durationInDays: Int,
    onApplyTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(text = "Current duration: $durationInDays")
        WCColoredButton(onClick = onApplyTapped, text = "Apply")
    }
}

@LightDarkThemePreviews
@Composable
private fun CampaignBudgetScreenPreview() {
    CampaignBudgetScreen(
        onBackPressed = {}
    )
}
