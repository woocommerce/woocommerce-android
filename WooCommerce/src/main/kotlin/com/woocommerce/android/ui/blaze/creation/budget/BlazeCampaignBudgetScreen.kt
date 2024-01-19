package com.woocommerce.android.ui.blaze.creation.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheetLayout
import com.woocommerce.android.ui.compose.component.WCTextButton
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

                EditBudgetSection(modifier = Modifier.weight(1f))
                EditDurationSection()
            }
        }
    }
}

@Composable
private fun EditBudgetSection(modifier: Modifier = Modifier) {
    var sliderValue by remember { mutableStateOf(35f) }

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
            text = stringResource(id = string.blaze_campaign_budget_subtitle),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center,
            color = colorResource(id = color.color_on_surface_medium)
        )
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = stringResource(id = string.blaze_campaign_budget_total_spend),
            style = MaterialTheme.typography.body1,
            color = colorResource(id = color.color_on_surface_medium)
        )
        Text(
            text = " $${sliderValue.toInt()} USD",
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(id = string.blaze_campaign_budget_days_duration),
            style = MaterialTheme.typography.h4,
            color = colorResource(id = color.color_on_surface_medium)
        )
        Text(
            modifier = Modifier.padding(top = 40.dp),
            text = stringResource(id = string.blaze_campaign_budget_daily_spend),
            color = colorResource(id = color.color_on_surface_medium)
        )
        Slider(
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            value = sliderValue,
            valueRange = 35f..350f,
            onValueChange = { sliderValue = it }
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(id = string.blaze_campaign_budget_reach_forecast))
            Icon(
                modifier = Modifier.padding(start = 4.dp),
                painter = painterResource(id = drawable.ic_info_outline_20dp),
                contentDescription = null
            )
        }
        Text(
            text = "2400  --  3000",
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EditDurationSection() {
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
                text = stringResource(id = string.blaze_campaign_budget_duration_section_title),
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
                    onClick = { /*TODO*/ }
                ) {
                    Text(text = stringResource(id = string.blaze_campaign_budget_edit_duration_button))
                }
            }
            WCColoredButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /*TODO*/ },
                text = stringResource(id = string.blaze_campaign_budget_update_button)
            )
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
