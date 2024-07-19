package com.woocommerce.android.ui.products.ai

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductType.SIMPLE

@Composable
fun ProductPreviewSubScreen(viewModel: ProductPreviewSubViewModel, modifier: Modifier) {
    viewModel.state.observeAsState().value?.let { state ->
        ProductPreviewSubScreen(
            state = state,
            onFeedbackReceived = viewModel::onFeedbackReceived,
            modifier = modifier
        )
    }
}

@Composable
private fun ProductPreviewSubScreen(
    state: ProductPreviewSubViewModel.State,
    onFeedbackReceived: (Boolean) -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_title),
            style = MaterialTheme.typography.h5
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_subtitle_legacy),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
        when (state) {
            ProductPreviewSubViewModel.State.Loading,
            is ProductPreviewSubViewModel.State.Error -> ProductPreviewLoading(
                modifier = Modifier.fillMaxHeight()
            )

            is ProductPreviewSubViewModel.State.Success -> ProductPreviewContent(
                state = state,
                onFeedbackReceived = onFeedbackReceived,
                modifier = Modifier.fillMaxHeight()
            )
        }

        if (state is ProductPreviewSubViewModel.State.Error) {
            ErrorDialog(
                onRetryClick = state.onRetryClick,
                onDismissClick = state.onDismissClick
            )
        }
    }
}

@Composable
private fun ProductPreviewContent(
    state: ProductPreviewSubViewModel.State.Success,
    onFeedbackReceived: (Boolean) -> Unit,
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
            text = stringResource(id = R.string.product_creation_ai_preview_name_section),
            style = MaterialTheme.typography.body2
        )
        Text(
            text = state.title,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
                .padding(dimensionResource(id = R.dimen.major_100))
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_short_description_section),
            style = MaterialTheme.typography.body2
        )
        Text(
            text = state.shortDescription,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
                .padding(dimensionResource(id = R.dimen.major_100))
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_description_section),
            style = MaterialTheme.typography.body2
        )
        Text(
            text = state.description,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
                .padding(dimensionResource(id = R.dimen.major_100))
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_details_section),
            style = MaterialTheme.typography.body2
        )

        state.propertyGroups.forEach { properties ->
            ProductProperties(properties = properties, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier)
        }

        AnimatedVisibility(
            visible = state.shouldShowFeedbackView,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(id = R.dimen.major_100))
        ) {
            AiFeedbackForm(
                onFeedbackReceived = onFeedbackReceived,
            )
        }
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
            text = stringResource(id = R.string.product_creation_ai_preview_name_section),
            style = MaterialTheme.typography.body2
        )
        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_short_description_section),
            style = MaterialTheme.typography.body2
        )
        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_description_section),
            style = MaterialTheme.typography.body2
        )
        LoadingSkeleton(
            lines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = R.string.product_creation_ai_preview_details_section),
            style = MaterialTheme.typography.body2
        )
        LoadingSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
        )
        Spacer(Modifier)
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
    onDismissClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false),
        text = {
            Text(text = stringResource(id = R.string.product_creation_ai_generation_failure_message))
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
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
private fun ProductPreviewLoadingPreview() {
    WooThemeWithBackground {
        ProductPreviewSubScreen(
            state = ProductPreviewSubViewModel.State.Loading,
            onFeedbackReceived = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
private fun ProductPreviewContentPreview() {
    WooThemeWithBackground {
        ProductPreviewSubScreen(
            state = ProductPreviewSubViewModel.State.Success(
                product = ProductHelper.getDefaultNewProduct(SIMPLE, false).copy(
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
                )
            ),
            onFeedbackReceived = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
