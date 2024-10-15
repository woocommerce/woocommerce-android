package com.woocommerce.android.ui.blaze.creation.objective

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.blaze.creation.objective.BlazeCampaignObjectiveViewModel.ObjectiveItem
import com.woocommerce.android.ui.blaze.creation.objective.BlazeCampaignObjectiveViewModel.ObjectiveViewState
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews

@Composable
fun BlazeCampaignObjectiveScreen(viewModel: BlazeCampaignObjectiveViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        ObjectiveScreen(
            state = state,
            onBackPressed = viewModel::onBackPressed,
            onSaveTapped = viewModel::onSaveTapped,
            onObjectiveTapped = viewModel::onItemToggled
        )
    }
}

@Composable
private fun ObjectiveScreen(
    state: ObjectiveViewState,
    onBackPressed: () -> Unit,
    onSaveTapped: () -> Unit,
    onObjectiveTapped: (ObjectiveItem) -> Unit,
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.blaze_campaign_preview_details_objective),
                onNavigationButtonClick = onBackPressed,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                actions = {
                    WCTextButton(
                        onClick = onSaveTapped,
                        enabled = state.isSaveButtonEnabled,
                        text = stringResource(R.string.save)
                    )
                }
            )
        },
        modifier = Modifier.background(MaterialTheme.colors.surface)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.minor_50))
            ) {
                items(state.items) {
                    ObjectiveListItem(
                        it.title,
                        it.description,
                        it.suitableForDescription,
                        onClickLabel = stringResource(
                            id = R.string.blaze_campaign_objective_select_objective_label,
                            it.title
                        ),
                        isSelected = it.id == state.selectedItemId,
                    ) {
                        onObjectiveTapped(it)
                    }
                }
            }
        }
    }
}

@Composable
fun ObjectiveListItem(
    title: String,
    description: String,
    suitableForDescription: String,
    isSelected: Boolean,
    onClickLabel: String?,
    onItemClick: () -> Unit,
) {
    val roundedShape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
    val borderSize = if (isSelected) R.dimen.minor_25 else R.dimen.minor_10
    val borderColor = if (isSelected) R.color.woo_purple_60 else R.color.woo_gray_5
    val modifier = Modifier
        .fillMaxWidth()
        .padding(
            horizontal = dimensionResource(id = R.dimen.major_100),
            vertical = dimensionResource(id = R.dimen.minor_50)
        )
        .clip(roundedShape)
        .clickable(
            role = Role.Button,
            onClick = { onItemClick() },
            onClickLabel = onClickLabel
        )
        .border(
            width = dimensionResource(id = borderSize),
            color = colorResource(id = borderColor),
            shape = roundedShape
        )

    Row(
        modifier = if (isSelected) {
            modifier.then(Modifier.background(colorResource(id = R.color.blaze_campaign_objective_item_background)))
        } else {
            modifier
        }.padding(dimensionResource(id = R.dimen.major_100)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100))
    ) {
        val selectionDrawable = if (isSelected) {
            R.drawable.ic_rounded_chcekbox_checked
        } else {
            R.drawable.ic_rounded_chcekbox_unchecked
        }
        Crossfade(
            targetState = selectionDrawable,
            label = "itemSelection"
        ) { icon ->
            Image(
                painter = painterResource(id = icon),
                contentDescription = null
            )
        }

        Column(
            modifier = Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colors.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface,
            )
            if (isSelected) {
                Text(
                    text = annotatedStringRes(
                        stringResId = R.string.blaze_campaign_objective_good_for,
                        suitableForDescription
                    ),
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface,
                )
            }
        }
    }
}

@LightDarkThemePreviews
@Composable
fun PreviewObjectiveScreen() {
    ObjectiveScreen(
        state = ObjectiveViewState(
            items = listOf(
                ObjectiveItem(
                    id = "traffic",
                    title = "Traffic",
                    description = "Aims to drive more visitors and increase page views.",
                    suitableForDescription = "E-commerce sites, content-driven websites, startups."
                ),
                ObjectiveItem(
                    id = "sales",
                    title = "Sales",
                    description = "Converts potential customers into buyers by encouraging purchase.",
                    suitableForDescription = "E-commerce, retailers, subscription services."
                ),
                ObjectiveItem(
                    id = "awareness",
                    title = "Awareness",
                    description = "Focuses on increasing brand recognition and visibility.",
                    suitableForDescription = "New businesses, brands launching new products."
                ),
                ObjectiveItem(
                    id = "engagement",
                    title = "Engagement",
                    description = "Encourages your audience to interact and connect with your brand.",
                    suitableForDescription = "Influencers and community builders looking for followers of the same" +
                        "interest."
                ),
            ),
            selectedItemId = null
        ),
        onBackPressed = { },
        onSaveTapped = { },
        onObjectiveTapped = { },
    )
}
