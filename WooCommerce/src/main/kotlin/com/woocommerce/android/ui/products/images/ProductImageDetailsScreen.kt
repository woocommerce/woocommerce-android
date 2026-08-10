package com.woocommerce.android.ui.products.images

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.DiscardChangesDialog
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ProductImageDetailsScreen(viewModel: ProductImageDetailsViewModel) {
    val state by viewModel.state.collectAsState()
    ProductImageDetailsScreen(
        state = state,
        onAltTextChanged = viewModel::onAltTextChanged,
        onNameChanged = viewModel::onNameChanged,
        onDoneClicked = viewModel::onDoneClicked,
        onBackButtonClick = viewModel::onBackClick
    )
}

@Composable
private fun ProductImageDetailsScreen(
    state: ProductImageDetailsViewModel.UiState,
    onAltTextChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onDoneClicked: () -> Unit,
    onBackButtonClick: () -> Unit
) {
    BackHandler { onBackButtonClick() }

    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(R.string.product_image_details_title),
                onNavigationButtonClick = onBackButtonClick,
                actions = {
                    WCTextButton(
                        onClick = onDoneClicked,
                        text = stringResource(R.string.done),
                        enabled = state.hasChanges
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.major_100))
        ) {
            AsyncImage(
                model = state.imageUrl,
                contentDescription = stringResource(R.string.product_image_content_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(fraction = 0.5f)
                    .aspectRatio(1f)
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.major_100)))

            WCOutlinedTextField(
                value = state.altText,
                onValueChange = onAltTextChanged,
                label = stringResource(R.string.product_image_details_alt_text_label),
                placeholderText = state.altTextPlaceholder,
                helperText = if (state.isAltTextRemovalBlocked) {
                    stringResource(R.string.product_image_details_alt_text_removal_blocked)
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.major_100)))

            WCOutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = stringResource(R.string.product_image_details_name_label),
                placeholderText = state.namePlaceholder,
                helperText = if (state.isNameRemovalBlocked) {
                    stringResource(R.string.product_image_details_name_removal_blocked)
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        state.discardChangesDialogState?.let {
            DiscardChangesDialog(
                discardButton = it.onDiscard,
                dismissButton = it.onCancel
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun ProductImageDetailsScreenPreview() {
    WooThemeWithBackground {
        ProductImageDetailsScreen(
            state = ProductImageDetailsViewModel.UiState(
                imageUrl = "https://example.com/image.jpg",
                altText = "A black t-shirt",
                name = "black-t-shirt"
            ),
            onAltTextChanged = {},
            onNameChanged = {},
            onDoneClicked = {},
            onBackButtonClick = {}
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun ProductImageDetailsScreenClearedAltTextPreview() {
    WooThemeWithBackground {
        ProductImageDetailsScreen(
            state = ProductImageDetailsViewModel.UiState(
                imageUrl = "https://example.com/image.jpg",
                altText = "",
                name = "black-t-shirt",
                altTextPlaceholder = "A black t-shirt",
                namePlaceholder = "black-t-shirt",
                isAltTextRemovalBlocked = true
            ),
            onAltTextChanged = {},
            onNameChanged = {},
            onDoneClicked = {},
            onBackButtonClick = {}
        )
    }
}
