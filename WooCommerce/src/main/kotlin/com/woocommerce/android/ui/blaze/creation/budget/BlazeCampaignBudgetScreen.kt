package com.woocommerce.android.ui.blaze.creation.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheetLayout
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
                title = stringResource(id = string.blaze_campaign_budget_title),
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
                    .padding(dimensionResource(id = dimen.major_100))
            ) {
                // Budget content
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
