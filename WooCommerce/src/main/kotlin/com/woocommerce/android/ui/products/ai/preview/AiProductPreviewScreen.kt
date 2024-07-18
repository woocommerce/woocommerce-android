package com.woocommerce.android.ui.products.ai.preview

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.autoMirror
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.ai.AIProductModel
import com.woocommerce.android.ui.products.ai.AiFeedbackForm
import com.woocommerce.android.ui.products.ai.ProductPropertyCard
import com.woocommerce.android.ui.products.ai.components.FullScreenImageViewer
import com.woocommerce.android.ui.products.ai.components.ImageAction
import com.woocommerce.android.ui.products.ai.components.SelectedImageSection

@Composable
fun AiProductPreviewScreen(viewModel: AiProductPreviewViewModel) {
    viewModel.state.observeAsState().value?.let { state ->
        AiProductPreviewScreen(
            state = state,
            onNameChanged = viewModel::onNameChanged,
            onDescriptionChanged = viewModel::onDescriptionChanged,
            onShortDescriptionChanged = viewModel::onShortDescriptionChanged,
            onFeedbackReceived = viewModel::onFeedbackReceived,
            onBackButtonClick = viewModel::onBackButtonClick,
            onImageActionSelected = viewModel::onImageActionSelected,
            onFullScreenImageDismissed = viewModel::onFullScreenImageDismissed,
            onSelectNextVariant = viewModel::onSelectNextVariant,
            onSelectPreviousVariant = viewModel::onSelectPreviousVariant,
            onSaveProductAsDraft = viewModel::onSaveProductAsDraft,
            onGenerateAgainClick = viewModel::onGenerateAgainClicked
        )
    }
}

@Composable
private fun AiProductPreviewScreen(
    state: AiProductPreviewViewModel.State,
    onNameChanged: (String?) -> Unit,
    onDescriptionChanged: (String?) -> Unit,
    onShortDescriptionChanged: (String?) -> Unit,
    onFeedbackReceived: (Boolean) -> Unit,
    onBackButtonClick: () -> Unit,
    onImageActionSelected: (ImageAction) -> Unit,
    onFullScreenImageDismissed: () -> Unit,
    onSelectNextVariant: () -> Unit,
    onSelectPreviousVariant: () -> Unit,
    onSaveProductAsDraft: () -> Unit,
    onGenerateAgainClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                onNavigationButtonClick = onBackButtonClick,
                actions = {
                    when {
                        state is AiProductPreviewViewModel.State.Success &&
                            state.savingProductState is AiProductPreviewViewModel.SavingProductState.Loading -> {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(
                                        width = dimensionResource(id = R.dimen.major_325),
                                        height = dimensionResource(id = R.dimen.major_100)
                                    )
                                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                            )
                        }

                        else -> {
                            WCTextButton(
                                enabled = state is AiProductPreviewViewModel.State.Success,
                                onClick = onSaveProductAsDraft
                            ) {
                                Text(text = stringResource(id = R.string.product_detail_save_as_draft))
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            Text(
                text = stringResource(id = R.string.product_creation_ai_preview_title),
                style = MaterialTheme.typography.h5
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            Text(
                text = stringResource(id = R.string.product_creation_ai_preview_subtitle),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
            when (state) {
                AiProductPreviewViewModel.State.Loading,
                is AiProductPreviewViewModel.State.Error -> ProductPreviewLoading(
                    modifier = Modifier.fillMaxHeight()
                )

                is AiProductPreviewViewModel.State.Success -> ProductPreviewContent(
                    state = state,
                    onNameChanged = onNameChanged,
                    onDescriptionChanged = onDescriptionChanged,
                    onShortDescriptionChanged = onShortDescriptionChanged,
                    onFeedbackReceived = onFeedbackReceived,
                    onImageActionSelected = onImageActionSelected,
                    onFullScreenImageDismissed = onFullScreenImageDismissed,
                    onSelectNextVariant = onSelectNextVariant,
                    onSelectPreviousVariant = onSelectPreviousVariant,
                    onGenerateAgainClick = onGenerateAgainClick,
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }

    if (state is AiProductPreviewViewModel.State.Error) {
        ErrorDialog(
            onRetryClick = state.onRetryClick,
            onDismissClick = state.onDismissClick,
            errorMessage = R.string.product_creation_ai_generation_failure_message
        )
    }
}

@Composable
private fun ProductPreviewContent(
    state: AiProductPreviewViewModel.State.Success,
    onNameChanged: (String?) -> Unit,
    onDescriptionChanged: (String?) -> Unit,
    onShortDescriptionChanged: (String?) -> Unit,
    onFeedbackReceived: (Boolean) -> Unit,
    onImageActionSelected: (ImageAction) -> Unit,
    onFullScreenImageDismissed: () -> Unit,
    onSelectNextVariant: () -> Unit,
    onSelectPreviousVariant: () -> Unit,
    onGenerateAgainClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        modifier = modifier
    ) {
        val sectionsBorder = Modifier.border(
            width = dimensionResource(id = R.dimen.minor_10),
            color = colorResource(id = R.color.divider_color),
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
        )

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_name_description_sections),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold
        )
        ProductTextField(
            state = state.name,
            selectedVariant = state.selectedVariant,
            onValueChange = onNameChanged,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )

        ProductTextField(
            state = state.shortDescription,
            selectedVariant = state.selectedVariant,
            onValueChange = onShortDescriptionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )

        ProductTextField(
            state = state.description,
            selectedVariant = state.selectedVariant,
            onValueChange = onDescriptionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )

        if (state.shouldShowVariantSelector) {
            Spacer(Modifier)
            ProductVariantSelector(
                selectedVariant = state.selectedVariant,
                totalVariants = state.variantsCount,
                onSelectNextVariant = onSelectNextVariant,
                onSelectPreviousVariant = onSelectPreviousVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (state.imageState.image != null) {
            Spacer(Modifier.height(8.dp))
            ProductImage(
                state = state.imageState,
                onImageActionSelected = onImageActionSelected,
                onFullScreenImageDismissed = onFullScreenImageDismissed,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_details_section),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold
        )

        state.propertyGroups.forEach { properties ->
            ProductProperties(properties = properties, modifier = Modifier.fillMaxWidth())
        }

        AnimatedVisibility(
            visible = state.shouldShowFeedbackView,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            AiFeedbackForm(
                onFeedbackReceived = onFeedbackReceived,
            )
        }

        WCOutlinedButton(
            onClick = onGenerateAgainClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(text = stringResource(id = R.string.product_creation_ai_preview_generate_again))
        }
    }
    if (state.savingProductState is AiProductPreviewViewModel.SavingProductState.Error) {
        ErrorDialog(
            errorMessage = state.savingProductState.messageRes,
            onRetryClick = state.savingProductState.onRetryClick,
            onDismissClick = state.savingProductState.onDismissClick
        )
    }
}

@Composable
private fun ProductTextField(
    state: AiProductPreviewViewModel.TextFieldState,
    selectedVariant: Int,
    onValueChange: (String?) -> Unit,
    modifier: Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(selectedVariant) {
        // Clear focus when the selected variant changes, otherwise the cursor will be at the wrong position
        // depending on the previous variant's text length
        focusManager.clearFocus()
    }

    Column(modifier) {
        BasicTextField(
            value = state.value,
            onValueChange = onValueChange,
            textStyle = TextStyle.Default.copy(
                color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .focusRequester(focusRequester)
        )

        AnimatedVisibility(state.isValueEditedManually) {
            Column {
                Divider()
                WCTextButton(
                    onClick = { onValueChange(null) },
                    text = stringResource(id = R.string.product_creation_ai_preview_undo_edits),
                    icon = Icons.Default.Replay,
                    allCaps = false
                )
            }
        }
    }
}

@Composable
private fun ProductVariantSelector(
    selectedVariant: Int,
    totalVariants: Int,
    onSelectNextVariant: () -> Unit,
    onSelectPreviousVariant: () -> Unit,
    modifier: Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Text(
            text = stringResource(
                id = R.string.product_creation_ai_preview_variant_selector,
                selectedVariant + 1,
                totalVariants
            ),
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onSelectPreviousVariant,
            enabled = selectedVariant > 0,
            modifier = Modifier
                .border(ButtonDefaults.outlinedBorder, shape = RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = stringResource(id = R.string.product_creation_ai_select_previous_option),
                tint = MaterialTheme.colors.primary.copy(alpha = LocalContentAlpha.current),
                modifier = Modifier
                    .size(32.dp)
                    .autoMirror()
            )
        }
        IconButton(
            onClick = onSelectNextVariant,
            enabled = selectedVariant < totalVariants - 1,
            modifier = Modifier
                .border(ButtonDefaults.outlinedBorder, shape = RoundedCornerShape(8.dp))
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(id = R.string.product_creation_ai_select_next_option),
                tint = MaterialTheme.colors.primary.copy(alpha = LocalContentAlpha.current),
                modifier = Modifier
                    .size(32.dp)
                    .autoMirror()
            )
        }
    }
}

@Composable
private fun ProductImage(
    state: AiProductPreviewViewModel.ImageState,
    onFullScreenImageDismissed: () -> Unit,
    onImageActionSelected: (ImageAction) -> Unit,
    modifier: Modifier
) {
    if (state.image == null) return

    SelectedImageSection(
        image = state.image,
        subtitle = stringResource(id = R.string.ai_product_creation_image_selected_subtitle),
        onImageActionSelected = onImageActionSelected,
        dropDownActions = listOf(ImageAction.View, ImageAction.Remove),
        modifier = modifier
            .background(
                color = colorResource(id = R.color.ai_generated_text_background),
                shape = RoundedCornerShape(8.dp)
            )
    )

    if (state.showImageFullScreen) {
        FullScreenImageViewer(
            image = state.image,
            onDismiss = onFullScreenImageDismissed
        )
    }
}

@Composable
private fun ProductProperties(
    properties: List<ProductPropertyCard>,
    modifier: Modifier
) {
    val borderWidth = dimensionResource(id = R.dimen.minor_10)
    val borderColor = colorResource(id = R.color.divider_color)
    Column(
        modifier.border(
            width = borderWidth,
            color = borderColor,
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
        )
    ) {
        properties.forEachIndexed { index, property ->
            Row(
                Modifier
                    .padding(dimensionResource(id = R.dimen.major_100))
            ) {
                Icon(
                    painter = painterResource(id = property.icon),
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_50))
                )
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = property.title),
                        style = MaterialTheme.typography.subtitle1,
                        color = colorResource(id = R.color.color_on_surface_high)
                    )
                    Text(
                        text = property.content,
                        style = MaterialTheme.typography.body2,
                        color = colorResource(id = R.color.color_on_surface_medium)
                    )
                }
            }

            if (index < properties.lastIndex) {
                Divider(color = borderColor, thickness = borderWidth)
            }
        }
    }
}

@Composable
private fun ProductPreviewLoading(modifier: Modifier) {
    @Composable
    fun LoadingSkeleton(lines: Int = 2, modifier: Modifier) {
        require(lines == 2 || lines == 3)
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            modifier = modifier
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            if (lines == 3) {
                SkeletonView(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(dimensionResource(id = R.dimen.major_100))
                )
            }
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth(if (lines == 3) 0.5f else 0.6f)
                    .height(dimensionResource(id = R.dimen.major_100))
            )
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth(if (lines == 3) 0.7f else 0.8f)
                    .height(dimensionResource(id = R.dimen.major_100))
            )
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
        modifier = modifier
    ) {
        val sectionsBorder = Modifier.border(
            width = dimensionResource(id = R.dimen.minor_10),
            color = colorResource(id = R.color.divider_color),
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
        )

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_name_description_sections),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold
        )
        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )

        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )

        LoadingSkeleton(
            lines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_details_section),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.SemiBold
        )
        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )
        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )
    }
}

@Composable
private fun ErrorDialog(
    onRetryClick: () -> Unit,
    onDismissClick: () -> Unit,
    @StringRes errorMessage: Int
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        text = {
            Text(text = stringResource(id = errorMessage))
        },
        confirmButton = {
            WCTextButton(onClick = onRetryClick) {
                Text(stringResource(id = R.string.retry))
            }
        },
        dismissButton = {
            WCTextButton(onClick = onDismissClick) {
                Text(stringResource(id = R.string.dismiss))
            }
        }
    )
}

@Composable
@Preview
@PreviewLightDark
private fun ProductPreviewLoadingPreview() {
    WooThemeWithBackground {
        AiProductPreviewScreen(
            state = AiProductPreviewViewModel.State.Loading,
            onNameChanged = {},
            onDescriptionChanged = {},
            onShortDescriptionChanged = {},
            onFeedbackReceived = {},
            onBackButtonClick = {},
            onImageActionSelected = {},
            onFullScreenImageDismissed = {},
            onSelectNextVariant = {},
            onSelectPreviousVariant = {},
            onSaveProductAsDraft = {},
            onGenerateAgainClick = {}
        )
    }
}

@Composable
@Preview
@PreviewLightDark
private fun ProductPreviewContentPreview() {
    WooThemeWithBackground {
        AiProductPreviewScreen(
            state = AiProductPreviewViewModel.State.Success(
                selectedVariant = 0,
                product = AIProductModel.buildDefault(
                    name = "Soft Black Tee: Elevate Your Everyday Style",
                    description = "Introducing our USA-Made Classic Organic Cotton Tee—a staple piece designed for" +
                        " everyday comfort and sustainability. Crafted with care from organic cotton, this tee is not" +
                        " just soft on your skin but gentle on the environment."
                ),
                propertyGroups = listOf(
                    listOf(
                        ProductPropertyCard(
                            icon = R.drawable.ic_gridicons_product,
                            title = R.string.product_type,
                            content = "Simple Product"
                        )
                    ),
                    listOf(
                        ProductPropertyCard(
                            icon = R.drawable.ic_gridicons_money,
                            title = R.string.product_price,
                            content = "Regular price: $45.00"
                        ),
                        ProductPropertyCard(
                            icon = R.drawable.ic_gridicons_list_checkmark,
                            title = R.string.product_inventory,
                            content = "In stock"
                        )
                    )
                ),
                imageState = AiProductPreviewViewModel.ImageState(
                    image = null,
                    showImageFullScreen = false,
                ),
                savingProductState = AiProductPreviewViewModel.SavingProductState.Idle,
            ),
            onNameChanged = {},
            onDescriptionChanged = {},
            onShortDescriptionChanged = {},
            onFeedbackReceived = {},
            onBackButtonClick = {},
            onImageActionSelected = {},
            onFullScreenImageDismissed = {},
            onSelectNextVariant = {},
            onSelectPreviousVariant = {},
            onSaveProductAsDraft = {},
            onGenerateAgainClick = {}
        )
    }
}
