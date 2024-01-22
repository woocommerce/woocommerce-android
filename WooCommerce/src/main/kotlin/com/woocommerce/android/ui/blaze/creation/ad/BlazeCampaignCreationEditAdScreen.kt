package com.woocommerce.android.ui.blaze.creation.ad

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.mediapicker.MediaPickerDialog
import com.woocommerce.android.ui.blaze.creation.ad.BlazeCampaignCreationEditAdViewModel.ViewState
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource

@Composable
fun BlazeCampaignCreationPreviewScreen(viewModel: BlazeCampaignCreationEditAdViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        BlazeCampaignCreationEditAdScreen(
            viewState = viewState,
            onTagLineChanged = viewModel::onTagLineChanged,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onChangeImageTapped = viewModel::onChangeImageTapped,
            onPreviousSuggestionTapped = viewModel::onPreviousSuggestionTapped,
            onNextSuggestionTapped = viewModel::onNextSuggestionTapped,
            onBackButtonTapped = viewModel::onBackButtonTapped,
            onMediaPickerDialogDismissed = viewModel::onMediaPickerDialogDismissed,
            onMediaLibraryRequested = viewModel::onMediaLibraryRequested,
            onSaveTapped = viewModel::onSaveTapped
        )
    }
}

@Composable
private fun BlazeCampaignCreationEditAdScreen(
    viewState: ViewState,
    onTagLineChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onChangeImageTapped: () -> Unit,
    onPreviousSuggestionTapped: () -> Unit,
    onNextSuggestionTapped: () -> Unit,
    onBackButtonTapped: () -> Unit,
    onMediaPickerDialogDismissed: () -> Unit,
    onMediaLibraryRequested: (DataSource) -> Unit,
    onSaveTapped: () -> Unit
) {
    if (viewState.isMediaPickerDialogVisible) {
        MediaPickerDialog(
            onMediaPickerDialogDismissed,
            onMediaLibraryRequested
        )
    }

    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = string.blaze_campaign_preview_edit_ad),
                onNavigationButtonClick = onBackButtonTapped,
                navigationIcon = Filled.ArrowBack,
                onActionButtonClick = onSaveTapped,
                actionButtonText = stringResource(id = string.save).uppercase()
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            CampaignEditAdContent(
                viewState,
                onTagLineChanged,
                onDescriptionChanged,
                onChangeImageTapped,
                onPreviousSuggestionTapped,
                onNextSuggestionTapped
            )
        }
    }
}

@Composable
fun CampaignEditAdContent(
    viewState: ViewState,
    onTagLineChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onChangeImageTapped: () -> Unit,
    onPreviousSuggestionTapped: () -> Unit,
    onNextSuggestionTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AdImageSection(modifier, viewState, onChangeImageTapped)

        Divider(
            color = colorResource(id = color.divider_color),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = dimensionResource(id = dimen.minor_100))
        )

        AdDataSection(
            modifier = modifier,
            viewState = viewState,
            onTagLineChanged = onTagLineChanged,
            onDescriptionChanged = onDescriptionChanged,
            onPreviousSuggestionTapped = onPreviousSuggestionTapped,
            onNextSuggestionTapped = onNextSuggestionTapped
        )
    }
}

@Composable
private fun AdDataSection(
    modifier: Modifier,
    viewState: ViewState,
    onTagLineChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onPreviousSuggestionTapped: () -> Unit,
    onNextSuggestionTapped: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(dimensionResource(id = dimen.major_100))
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WCOutlinedTextField(
            value = viewState.tagLine,
            onValueChange = onTagLineChanged,
            label = stringResource(id = string.blaze_campaign_edit_ad_change_tagline_title),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )

        Text(
            text = stringResource(
                id = string.blaze_campaign_edit_ad_characters_remaining,
                viewState.taglineCharactersRemaining
            ),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = color.color_on_surface_disabled),
            modifier = Modifier
                .padding(top = dimensionResource(id = dimen.minor_100))
                .fillMaxWidth()
        )

        WCOutlinedTextField(
            value = viewState.description,
            onValueChange = onDescriptionChanged,
            label = stringResource(id = string.blaze_campaign_edit_ad_change_description_title),
            maxLines = 3,
            minLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier
                .padding(top = dimensionResource(id = dimen.major_150))
        )

        Text(
            text = stringResource(
                id = string.blaze_campaign_edit_ad_characters_remaining,
                viewState.descriptionCharactersRemaining
            ),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = color.color_on_surface_disabled),
            modifier = Modifier
                .padding(top = dimensionResource(id = dimen.minor_100))
                .fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .padding(top = dimensionResource(id = dimen.major_100))
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = drawable.ic_ai),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorResource(id = color.color_on_surface)),
                modifier = Modifier
                    .size(dimensionResource(id = dimen.image_minor_80))
                    .padding(end = dimensionResource(id = dimen.minor_100))
            )
            Text(
                text = stringResource(id = string.blaze_campaign_edit_ad_suggested_by_ai),
                style = MaterialTheme.typography.subtitle2,
            )
            Spacer(modifier = Modifier.weight(1f))

            SuggestionButton(
                onClick = onPreviousSuggestionTapped,
                isEnabled = viewState.isPreviousSuggestionButtonEnabled,
                icon = Filled.ArrowBackIosNew
            )
            SuggestionButton(
                onClick = onNextSuggestionTapped,
                isEnabled = viewState.isNextSuggestionButtonEnabled,
                icon = Filled.ArrowForwardIos,
                modifier.padding(start = dimensionResource(id = dimen.major_150))
            )
        }
    }
}

@Composable
private fun AdImageSection(modifier: Modifier, viewState: ViewState, onChangeImageTapped: () -> Unit) {
    Column(
        modifier = modifier
            .padding(dimensionResource(id = dimen.major_100))
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = modifier
                .padding(dimensionResource(id = dimen.major_100))
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SubcomposeAsyncImage(
                model = Builder(LocalContext.current)
                    .data(viewState.adImageUrl)
                    .crossfade(true)
                    .fallback(drawable.blaze_campaign_product_placeholder)
                    .placeholder(drawable.blaze_campaign_product_placeholder)
                    .error(drawable.blaze_campaign_product_placeholder)
                    .build(),
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .size(dimensionResource(id = dimen.image_major_140))
                    .clip(shape = RoundedCornerShape(size = dimensionResource(id = dimen.minor_100))),
                loading = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(dimensionResource(id = dimen.progress_bar_small))
                        )
                    }
                }
            )

            WCTextButton(
                modifier = Modifier.padding(top = 8.dp),
                onClick = onChangeImageTapped,
            ) {
                Text(stringResource(id = string.blaze_campaign_edit_ad_change_image_button))
            }
        }
    }
}

@Composable
private fun SuggestionButton(
    onClick: () -> Unit,
    isEnabled: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = colorResource(id = color.image_border_color),
                shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100))
            )
            .clip(RoundedCornerShape(dimensionResource(id = dimen.minor_100)))
            .clickable(onClick = onClick, enabled = isEnabled)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isEnabled)
                MaterialTheme.colors.primary
            else
                colorResource(id = color.color_on_surface_disabled),
            modifier = Modifier
                .align(Alignment.Center)
                .size(dimensionResource(id = dimen.major_200))
                .padding(dimensionResource(id = dimen.minor_75))
        )
    }
}

@LightDarkThemePreviews
@Preview
@Composable
fun PreviewCampaignEditAdContent() {
    WooThemeWithBackground {
        CampaignEditAdContent(
            viewState = ViewState(
                tagLine = "From 45.00 USD",
                description = "Get the latest white t-shirts",
                adImageUrl = "https://rb.gy/gmjuwb"
            ),
            onTagLineChanged = { },
            onDescriptionChanged = { },
            onChangeImageTapped = { },
            onPreviousSuggestionTapped = { },
            onNextSuggestionTapped = { }
        )
    }
}
