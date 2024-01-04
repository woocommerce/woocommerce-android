package com.woocommerce.android.ui.blaze.creation.intro

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.ModalBottomSheetValue.HalfExpanded
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheetLayout
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import kotlinx.coroutines.launch

@Composable
fun BlazeCampaignCreationIntroScreen(
    viewModel: BlazeCampaignCreationIntroViewModel
) {
    WooThemeWithBackground {
        BlazeCampaignCreationIntroScreen(
            onContinueClick = viewModel::onContinueClick,
            onDismissClick = viewModel::onDismissClick
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BlazeCampaignCreationIntroScreen(
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onDismissClick,
                navigationIcon = Icons.Default.Clear
            )
        },
        modifier = Modifier.background(MaterialTheme.colors.surface)
    ) { paddingValues ->
        val coroutineScope = rememberCoroutineScope()
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = Hidden,
            confirmValueChange = { it != HalfExpanded }
        )

        WCModalBottomSheetLayout(
            sheetContent = {
                BlazeCampaignBottomSheetContent(
                    onDismissClick = {
                        coroutineScope.launch { modalSheetState.hide() }
                    }
                )
            },
            sheetState = modalSheetState,
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
        ) {
            BlazeCampaignCreationIntroContent(
                onContinueClick = onContinueClick,
                onLearnMoreClick = {
                    coroutineScope.launch { modalSheetState.show() }
                }
            )
        }
    }
}

@Composable
private fun BlazeCampaignCreationIntroContent(
    onContinueClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_blaze_monochrome),
                    tint = MaterialTheme.colors.primary,
                    contentDescription = null
                )

                Text(
                    text = stringResource(id = R.string.blaze),
                    color = MaterialTheme.colors.primary,
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = stringResource(id = R.string.blaze_campaign_creation_new_intro_title),
                style = MaterialTheme.typography.h4,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(id = R.string.blaze_campaign_creation_new_intro_description),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center
            )

            Image(
                painter = painterResource(id = R.drawable.img_blaze_intro),
                contentDescription = null
            )

            BlazeCampaignBenefitPoint(
                title = stringResource(id = R.string.blaze_campaign_creation_new_intro_benefit_1_title),
                subtitle = stringResource(id = R.string.blaze_campaign_creation_new_intro_benefit_1_subtitle),
                modifier = Modifier.fillMaxWidth()
            )

            BlazeCampaignBenefitPoint(
                title = stringResource(id = R.string.blaze_campaign_creation_new_intro_benefit_2_title),
                subtitle = stringResource(id = R.string.blaze_campaign_creation_new_intro_benefit_2_subtitle),
                modifier = Modifier.fillMaxWidth()
            )

            BlazeCampaignBenefitPoint(
                title = stringResource(id = R.string.blaze_campaign_creation_new_intro_benefit_3_title),
                subtitle = stringResource(id = R.string.blaze_campaign_creation_new_intro_benefit_3_subtitle),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Divider()

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        WCColoredButton(
            onClick = onContinueClick,
            text = stringResource(id = R.string.blaze_campaign_creation_new_intro_start_button),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )
        WCTextButton(
            onClick = onLearnMoreClick,
            text = stringResource(id = R.string.blaze_campaign_creation_new_intro_learn_more_button),
            allCaps = false,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    }
}

@Composable
private fun BlazeCampaignBenefitPoint(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Icon(
            painter = painterResource(id = R.drawable.ic_blaze_monochrome),
            tint = MaterialTheme.colors.primary,
            contentDescription = null
        )
        Spacer(modifier = Modifier.padding(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = Companion.Medium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun BlazeCampaignBottomSheetContent(
    onDismissClick: () -> Unit
) {
    // TODO
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BlazeCampaignCreationIntroScreenPreview() {
    WooThemeWithBackground {
        BlazeCampaignCreationIntroScreen(
            onContinueClick = {},
            onDismissClick = {}
        )
    }
}
