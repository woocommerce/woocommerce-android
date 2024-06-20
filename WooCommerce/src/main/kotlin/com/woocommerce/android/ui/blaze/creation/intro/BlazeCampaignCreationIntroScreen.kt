package com.woocommerce.android.ui.blaze.creation.intro

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontWeight.Companion
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
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
            onDismissClick = viewModel::onDismissClick,
            onLearnMoreClick = viewModel::onLearnMoreClick
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BlazeCampaignCreationIntroScreen(
    onContinueClick: () -> Unit,
    onDismissClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
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
            confirmValueChange = { it != HalfExpanded },
            skipHalfExpanded = true
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
                    onLearnMoreClick()
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50))
            ) {
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
                subtitle = stringResource(id = R.string.blaze_campaign_creation_new_intro_benefit_1_subtitle_updated),
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
            text = stringResource(id = R.string.blaze_campaign_creation_new_intro_learn_more),
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
        Spacer(modifier = Modifier.padding(dimensionResource(id = R.dimen.minor_50)))
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = Companion.Medium
        )
        Text(
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.minor_50)),
            text = subtitle,
            style = MaterialTheme.typography.body2
        )
    }
}

@Composable
private fun BlazeCampaignBottomSheetContent(
    onDismissClick: () -> Unit
) {
    val learnMoreItems = listOf(
        annotatedStringRes(R.string.blaze_campaign_creation_new_intro_learn_item_1),
        annotatedStringRes(R.string.blaze_campaign_creation_new_intro_learn_item_2),
        annotatedStringRes(R.string.blaze_campaign_creation_new_intro_learn_item_3),
        annotatedStringRes(R.string.blaze_campaign_creation_new_intro_learn_item_4),
        annotatedStringRes(R.string.blaze_campaign_creation_new_intro_learn_item_5),
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        BottomSheetHandle(
            modifier = Modifier
                .padding(bottom = dimensionResource(id = R.dimen.major_100))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.blaze_campaign_creation_new_intro_learn_more),
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
            )

            CloseButton(onDismissClick, modifier = Modifier.align(Alignment.CenterStart))
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))

        learnMoreItems.forEachIndexed { i, item ->
            if (i > 0) {
                Divider(color = colorResource(id = R.color.color_surface_elevated), thickness = 1.dp)
            }

            StepItem(
                currentStep = i,
                steps = learnMoreItems.size,
                text = item
            )
        }
    }
}

@Composable
private fun CloseButton(onDismissClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clickable(
                onClick = onDismissClick,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = dimensionResource(id = R.dimen.major_150))
            )
    ) {
        Icon(
            Icons.Filled.Close,
            contentDescription = stringResource(R.string.close),
            tint = colorResource(id = R.color.color_on_surface_medium),
            modifier = Modifier
                .align(Alignment.Center)
                .size(dimensionResource(id = R.dimen.major_150))
        )
    }
}

@Composable
fun CircleNumber(number: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(dimensionResource(id = R.dimen.major_150))
            .background(color = colorResource(id = R.color.color_surface_elevated), shape = CircleShape)
    ) {
        Text(
            text = number,
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.caption,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun StepItem(currentStep: Int, steps: Int, text: AnnotatedString) {
    val shape = when {
        currentStep == 0 -> RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.minor_100),
            topEnd = dimensionResource(id = R.dimen.minor_100)
        )

        currentStep + 1 == steps -> RoundedCornerShape(
            bottomStart = dimensionResource(id = R.dimen.minor_100),
            bottomEnd = dimensionResource(id = R.dimen.minor_100)
        )

        else -> RoundedCornerShape(0.dp)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = colorResource(id = R.color.blaze_campaign_bottom_sheet_highlight_background),
                shape = shape
            )
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        CircleNumber(number = (currentStep + 1).toString())
        Text(
            text = text,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100))
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BlazeCampaignCreationIntroScreenPreview() {
    WooThemeWithBackground {
        BlazeCampaignCreationIntroScreen(
            onContinueClick = {},
            onDismissClick = {},
            onLearnMoreClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun BlazeCampaignBottomSheetContentPreview() {
    WooThemeWithBackground {
        BlazeCampaignBottomSheetContent(
            onDismissClick = {}
        )
    }
}
