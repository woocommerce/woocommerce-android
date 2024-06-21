package com.woocommerce.android.ui.blaze.creation.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.woocommerce.android.extensions.formatToMMMddYYYY
import com.woocommerce.android.ui.blaze.creation.budget.BlazeCampaignBudgetViewModel.Companion.MAX_DATE_LIMIT_IN_DAYS
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.DatePickerDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheetLayout
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

@Composable
fun CampaignBudgetScreen(viewModel: BlazeCampaignBudgetViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        CampaignBudgetScreen(
            state = viewState,
            onBackPressed = viewModel::onBackPressed,
            onEditDurationTapped = viewModel::onEditDurationTapped,
            onImpressionsInfoTapped = viewModel::onImpressionsInfoTapped,
            onBudgetUpdated = viewModel::onBudgetUpdated,
            onStartDateChanged = viewModel::onStartDateChanged,
            onBudgetChangeFinished = viewModel::onBudgetChangeFinished,
            onUpdateTapped = viewModel::onUpdateTapped,
            onApplyDurationTapped = viewModel::onApplyDurationTapped
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
    onStartDateChanged: (Long) -> Unit,
    onBudgetChangeFinished: () -> Unit,
    onUpdateTapped: () -> Unit,
    onApplyDurationTapped: (Int) -> Unit
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
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
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
                            budgetUiState = state,
                            onStartDateChanged = { onStartDateChanged(it) },
                            onApplyTapped = {
                                onApplyDurationTapped(it)
                                coroutineScope.launch { modalSheetState.hide() }
                            }
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
                    onBudgetChangeFinished = onBudgetChangeFinished,
                    modifier = Modifier.weight(1f)
                )
                EditDurationSection(
                    campaignDurationDates = state.campaignDurationDates,
                    onEditDurationTapped = {
                        onEditDurationTapped()
                        coroutineScope.launch { modalSheetState.show() }
                    },
                    onUpdateTapped = onUpdateTapped
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
    onBudgetChangeFinished: () -> Unit,
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
            value = state.totalBudget,
            valueRange = state.budgetRangeMin..state.budgetRangeMax,
            onValueChange = { onBudgetUpdated(it) },
            onValueChangeFinished = { onBudgetChangeFinished() },
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
        Spacer(modifier = Modifier.height(6.dp))
        if (state.forecast.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(20.dp),
            )
        } else {
            if (state.forecast.isError) {
                Text(
                    modifier = Modifier.clickable { onBudgetChangeFinished() },
                    text = stringResource(id = R.string.blaze_campaign_budget_error_fetching_forecast),
                    color = colorResource(id = color.color_on_surface_medium)
                )
            } else {
                Text(
                    text = "${state.forecast.impressionsMin} - ${state.forecast.impressionsMax}",
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 24.sp,
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EditDurationSection(
    campaignDurationDates: String,
    onEditDurationTapped: () -> Unit,
    onUpdateTapped: () -> Unit,
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
                    text = campaignDurationDates,
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
                onClick = onUpdateTapped,
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
    budgetUiState: BlazeCampaignBudgetViewModel.BudgetUiState,
    onStartDateChanged: (Long) -> Unit,
    onApplyTapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        DatePickerDialog(
            currentDate = Date(budgetUiState.confirmedCampaignStartDateMillis),
            minDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time,
            maxDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, MAX_DATE_LIMIT_IN_DAYS) }.time,
            onDateSelected = {
                onStartDateChanged(it.time)
                showDatePicker = false
            },
            onDismissRequest = { showDatePicker = false }
        )
    }

    var sliderPosition by remember { mutableStateOf(budgetUiState.durationInDays.toFloat()) }
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
            text = stringResource(
                id = R.string.blaze_campaign_budget_duration_bottom_sheet_duration,
                sliderPosition.toInt()
            ),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Slider(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            value = sliderPosition,
            valueRange = budgetUiState.durationRangeMin..budgetUiState.durationRangeMax,
            onValueChange = { sliderPosition = it },
            colors = SliderDefaults.colors(
                inactiveTrackColor = colorResource(id = color.divider_color)
            )
        )
        Row(
            modifier = Modifier.padding(top = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.blaze_campaign_budget_duration_bottom_sheet_starts),
                style = MaterialTheme.typography.body1,
            )
            Text(
                modifier = Modifier
                    .clickable { showDatePicker = !showDatePicker }
                    .clip(RoundedCornerShape(4.dp))
                    .background(colorResource(id = color.divider_color))
                    .padding(8.dp),
                text = Date(budgetUiState.bottomSheetCampaignStartDateMillis).formatToMMMddYYYY(),
                style = MaterialTheme.typography.body1,
            )
        }
        WCColoredButton(
            modifier = Modifier
                .padding(
                    top = 30.dp,
                    bottom = 16.dp
                )
                .fillMaxWidth(),
            onClick = { onApplyTapped(sliderPosition.toInt()) },
            text = stringResource(id = R.string.blaze_campaign_budget_duration_bottom_sheet_apply_button)
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun CampaignBudgetScreenPreview() {
    CampaignBudgetScreen(
        state = BlazeCampaignBudgetViewModel.BudgetUiState(
            currencyCode = "USD",
            totalBudget = 35f,
            budgetRangeMin = 5f,
            budgetRangeMax = 35f,
            dailySpending = "$5",
            durationInDays = 7,
            durationRangeMin = 1f,
            durationRangeMax = 28f,
            forecast = BlazeCampaignBudgetViewModel.ForecastUi(
                isLoading = false,
                impressionsMin = 0,
                impressionsMax = 0,
                isError = false
            ),
            confirmedCampaignStartDateMillis = Date().time,
            bottomSheetCampaignStartDateMillis = Date().time,
            campaignDurationDates = "Dec 13 - Dec 20, 2023",
            showImpressionsBottomSheet = false,
            showCampaignDurationBottomSheet = false
        ),
        onBackPressed = {},
        onEditDurationTapped = {},
        onImpressionsInfoTapped = {},
        onBudgetUpdated = {},
        onStartDateChanged = {},
        onUpdateTapped = {},
        onBudgetChangeFinished = {},
        onApplyDurationTapped = {}
    )
}

@LightDarkThemePreviews
@Composable
private fun CampaignImpressionsBottomSheetPreview() {
    ImpressionsInfoBottomSheet(onDoneTapped = {})
}
