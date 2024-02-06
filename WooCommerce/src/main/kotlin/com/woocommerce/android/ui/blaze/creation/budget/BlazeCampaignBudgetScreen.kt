package com.woocommerce.android.ui.blaze.creation.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheetLayout
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import kotlinx.coroutines.launch

@Composable
fun CampaignBudgetScreen(viewModel: BlazeCampaignBudgetViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        CampaignBudgetScreen(
            state = viewState,
            onBackPressed = viewModel::onBackPressed,
            onEditDurationTapped = viewModel::onEditDurationTapped,
            onImpressionsInfoTapped = viewModel::onImpressionsInfoTapped,
            onBudgetUpdated = viewModel::onBudgetUpdated,
            onCampaignDurationUpdated = viewModel::onCampaignDurationUpdated,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CampaignBudgetScreen(
    state: BlazeCampaignBudgetViewModel.BudgetUiState,
    onBackPressed: () -> Unit,
    onEditDurationTapped: () -> Unit,
    onImpressionsInfoTapped: () -> Unit,
    onBudgetUpdated: (Float) -> Unit,
    onCampaignDurationUpdated: (Int) -> Unit,
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
                Column {
                    Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
                    BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
                    when {
                        state.showImpressionsBottomSheet -> ImpressionsInfoBottomSheet(
                            onDoneTapped = { coroutineScope.launch { modalSheetState.hide() } }
                        )

                        state.showCampaignDurationBottomSheet -> EditDurationBottomSheet(
                            duration = state.durationInDays,
                            startDate = state.startDateMmmDdYyyy,
                            onDurationChanged = { onCampaignDurationUpdated(it.toInt()) },
                            durationRange = state.durationRangeDays,
                            onApplyTapped = { coroutineScope.launch { modalSheetState.hide() } }
                        )
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .background(MaterialTheme.colors.surface)
            ) {
                EditBudgetSection(
                    state = state,
                    onImpressionsInfoTapped = {
                        onImpressionsInfoTapped()
                        coroutineScope.launch { modalSheetState.show() }
                    },
                    onBudgetUpdated = onBudgetUpdated,
                    modifier = Modifier.weight(1f)
                )
                EditDurationSection(
                    onEditDurationTapped = {
                        onEditDurationTapped()
                        coroutineScope.launch { modalSheetState.show() }
                    }
                )
            }
        }
    }
}

@Composable
private fun EditBudgetSection(
    state: BlazeCampaignBudgetViewModel.BudgetUiState,
    onBudgetUpdated: (Float) -> Unit,
    onImpressionsInfoTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(
            start = 28.dp,
            end = 28.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(
                top = 40.dp,
                bottom = 90.dp
            ),
            text = stringResource(id = R.string.blaze_campaign_budget_subtitle),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            color = colorResource(id = color.color_on_surface_medium)
        )
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = stringResource(id = R.string.blaze_campaign_budget_total_spend),
            style = MaterialTheme.typography.body1,
            color = colorResource(id = color.color_on_surface_medium)
        )
        Text(
            text = " $${state.totalBudget.toInt()} USD",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(id = R.string.blaze_campaign_budget_days_duration, state.durationInDays),
            style = MaterialTheme.typography.h4,
            color = colorResource(id = color.color_on_surface_medium)
        )
        Text(
            modifier = Modifier.padding(top = 40.dp),
            text = stringResource(id = R.string.blaze_campaign_budget_daily_spend, state.dailySpending),
            color = colorResource(id = color.color_on_surface_medium),
            style = MaterialTheme.typography.subtitle1,
        )
        Slider(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            value = state.sliderValue,
            valueRange = state.budgetRange,
            onValueChange = { onBudgetUpdated(it) },
            colors = SliderDefaults.colors(
                inactiveTrackColor = colorResource(id = color.divider_color)
            )
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(id = R.string.blaze_campaign_budget_reach_forecast))
            Icon(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .clickable { onImpressionsInfoTapped() },
                painter = painterResource(id = drawable.ic_info_outline_20dp),
                contentDescription = null
            )
        }
        Text(
            text = "2400  --  3000",
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EditDurationSection(
    onEditDurationTapped: () -> Unit
) {
    Column {
        Divider()
        Column(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 24.dp
            )
        ) {
            Text(
                text = stringResource(id = R.string.blaze_campaign_budget_duration_section_title),
                style = MaterialTheme.typography.body1,
                color = colorResource(id = color.color_on_surface_medium)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Dec 13 – Dec 19, 2023",
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = FontWeight.SemiBold,
                )
                WCTextButton(
                    onClick = onEditDurationTapped
                ) {
                    Text(text = stringResource(id = R.string.blaze_campaign_budget_edit_duration_button))
                }
            }
            WCColoredButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* TODO */ },
                text = stringResource(id = R.string.blaze_campaign_budget_update_button)
            )
        }
    }
}

@Composable
private fun ImpressionsInfoBottomSheet(
    onDoneTapped: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.blaze_campaign_budget_impressions_title),
                style = MaterialTheme.typography.h6,
            )
            WCTextButton(
                onClick = onDoneTapped
            ) {
                Text(text = stringResource(id = R.string.blaze_campaign_budget_impressions_done_button))
            }
        }
        Divider()
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(id = R.string.blaze_campaign_budget_impressions_info),
            style = MaterialTheme.typography.body1,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EditDurationBottomSheet(
    duration: Int,
    startDate: String,
    durationRange: ClosedFloatingPointRange<Float>,
    onDurationChanged: (Float) -> Unit,
    onApplyTapped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = stringResource(id = R.string.blaze_campaign_budget_duration_bottom_sheet_title),
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            modifier = Modifier
                .padding(top = 40.dp)
                .fillMaxWidth(),
            text = stringResource(id = R.string.blaze_campaign_budget_duration_bottom_sheet_duration, duration),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Slider(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            value = duration.toFloat(),
            valueRange = durationRange,
            onValueChange = onDurationChanged,
            colors = SliderDefaults.colors(
                inactiveTrackColor = colorResource(id = color.divider_color)
            )
        )
        Row(
            modifier = Modifier.padding(top = 40.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.blaze_campaign_budget_duration_bottom_sheet_starts),
                style = MaterialTheme.typography.body1,
            )
            Text(
                text = startDate,
                style = MaterialTheme.typography.body1,
            )
        }
        WCColoredButton(
            modifier = Modifier
                .padding(top = 40.dp)
                .fillMaxWidth(),
            onClick = onApplyTapped,
            text = stringResource(id = R.string.blaze_campaign_budget_duration_bottom_sheet_apply_button)
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun CampaignBudgetScreenPreview() {
    CampaignBudgetScreen(
        state = BlazeCampaignBudgetViewModel.BudgetUiState(
            totalBudget = 35f,
            sliderValue = 35f,
            spentBudget = 0f,
            budgetRange = 35f..350f,
            currencyCode = "USD",
            durationInDays = 7,
            dailySpending = "$5",
            startDateMmmDdYyyy = "Dec 13, 2023",
            durationRangeDays = 1f..28f,
            forecast = BlazeCampaignBudgetViewModel.ForecastUi(
                isLoaded = false,
                impressionsMin = 0,
                impressionsMax = 0
            )
        ),
        onBackPressed = {},
        onEditDurationTapped = {},
        onImpressionsInfoTapped = {},
        onBudgetUpdated = {},
        onCampaignDurationUpdated = {},
    )
}

@LightDarkThemePreviews
@Composable
private fun CampaignImpressionsBottomSheetPreview() {
    ImpressionsInfoBottomSheet(onDoneTapped = {})
}

@LightDarkThemePreviews
@Composable
private fun EditDurationBottomSheetPreview() {
    EditDurationBottomSheet(
        duration = 7,
        startDate = "Dec 13, 2023",
        durationRange = 1f..28f,
        onDurationChanged = {},
        onApplyTapped = {},
    )
}
