package com.woocommerce.android.ui.products.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooBadge
import com.woocommerce.android.ui.compose.designsystem.component.WooBadgeTone
import com.woocommerce.android.ui.compose.designsystem.component.WooCell
import com.woocommerce.android.ui.compose.designsystem.component.WooCellTrailingAffordance
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledTonalButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.Plus
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.compose.designsystem.icons.Xmark
import kotlinx.coroutines.delay

@Composable
fun ProductDetailScreen(
    state: ProductDetailScreenState,
    onLinkedProductPromoClicked: () -> Unit,
    onLinkedProductPromoDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = WooTheme.colors.background.section,
    ) {
        when (state) {
            ProductDetailScreenState.Loading -> ProductDetailLoading()
            is ProductDetailScreenState.Empty -> state.message?.let { ProductDetailError(it) } ?: ProductDetailEmpty()
            is ProductDetailScreenState.Error -> ProductDetailError(state.message)
            is ProductDetailScreenState.Content -> ProductDetailContent(
                state = state,
                onLinkedProductPromoClicked = onLinkedProductPromoClicked,
                onLinkedProductPromoDismissed = onLinkedProductPromoDismissed,
            )
        }
    }
}

@Composable
private fun ProductDetailEmpty() {
    Box(
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ProductDetailContent(
    state: ProductDetailScreenState.Content,
    onLinkedProductPromoClicked: () -> Unit,
    onLinkedProductPromoDismissed: () -> Unit,
) {
    val listState = rememberLazyListState()
    val nestedScrollInterop = rememberNestedScrollInteropConnection()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollInterop)
            .testTag(ProductDetailTestTags.LIST),
    ) {
        if (state.showLinkedProductPromo) {
            item(key = LINKED_PROMO_KEY) {
                LinkedProductPromo(
                    onClick = onLinkedProductPromoClicked,
                    onDismiss = onLinkedProductPromoDismissed,
                )
            }
        }
        state.cards.forEach { card ->
            productDetailCard(card)
        }
    }
}

@Composable
internal fun ProductDetailFooter(
    state: ProductDetailScreenState,
    onAddMoreClicked: () -> Unit,
) {
    if (state is ProductDetailScreenState.Content && state.showAddMore) {
        ProductDetailAddMore(onClick = onAddMoreClicked)
    }
}

private fun LazyListScope.productDetailCard(card: ProductDetailCardUiModel) {
    if (card.caption.isNotBlank()) {
        item(key = productDetailItemKey(card.key, CARD_CAPTION_KEY)) {
            Surface(
                color = WooTheme.colors.surface.bright,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    Text(
                        text = card.caption,
                        color = WooTheme.colors.surface.onDefault,
                        style = WooTheme.text.titleMedium.emphasized,
                        modifier = Modifier.padding(
                            horizontal = WooTheme.padding.padding7,
                            vertical = WooTheme.padding.padding4,
                        ),
                    )
                    WooDivider(modifier = Modifier.padding(start = WooTheme.padding.padding7))
                }
            }
        }
    }
    items(
        items = card.rows,
        key = { row -> productDetailItemKey(card.key, row.key) },
    ) { row ->
        Surface(
            color = WooTheme.colors.surface.bright,
            modifier = Modifier.fillMaxWidth(),
        ) {
            ProductDetailRow(row)
        }
    }
    item(key = productDetailItemKey(card.key, CARD_SPACER_KEY)) {
        Spacer(modifier = Modifier.height(WooTheme.spacing.space3))
    }
}

internal fun productDetailItemKey(cardKey: String, itemKey: String) = "$cardKey:$itemKey"

@Composable
private fun ProductDetailAddMore(onClick: () -> Unit) {
    Surface(
        color = WooTheme.colors.surface.bright,
        shadowElevation = ADD_MORE_ELEVATION,
        modifier = Modifier.fillMaxWidth(),
    ) {
        WooCell(
            title = stringResource(R.string.product_detail_add_more),
            onClick = onClick,
            leadingContent = {
                Icon(
                    imageVector = WooIcons.Regular.Plus,
                    contentDescription = null,
                    tint = WooTheme.colors.primary,
                )
            },
            trailingContent = { WooCellTrailingAffordance() },
        )
    }
}

@Composable
private fun LinkedProductPromo(
    onClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooTheme.padding.padding5),
        color = WooTheme.colors.container.secondaryContainer,
        contentColor = WooTheme.colors.container.onSecondaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(WooTheme.radius.large),
    ) {
        Row(
            modifier = Modifier.padding(WooTheme.padding.padding5),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            ) {
                WooBadge(text = stringResource(R.string.tip), tone = WooBadgeTone.Info)
                Text(
                    text = stringResource(R.string.promo_linked_products_banner_title),
                    style = WooTheme.text.titleMedium.emphasized,
                )
                Text(
                    text = stringResource(R.string.promo_linked_products_banner_message),
                    style = WooTheme.text.bodyMedium.regular,
                )
                WooFilledTonalButton(
                    text = stringResource(R.string.set_up_now),
                    onClick = onClick,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = WooIcons.Regular.Xmark,
                    contentDescription = stringResource(R.string.dismiss),
                )
            }
        }
    }
}

@Composable
private fun ProductDetailLoading() {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADING_DELAY_MS)
        isVisible = true
    }

    if (isVisible) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WooTheme.padding.padding7),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
        ) {
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth(SKELETON_TITLE_WIDTH)
                    .height(SKELETON_TITLE_HEIGHT)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(WooTheme.radius.medium)),
            )
            repeat(SKELETON_ROW_COUNT) {
                SkeletonView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SKELETON_ROW_HEIGHT)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(WooTheme.radius.medium)),
                )
            }
        }
    }
}

@Composable
private fun ProductDetailError(@androidx.annotation.StringRes message: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooTheme.padding.padding7),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.img_woo_generic_error),
            contentDescription = null,
            modifier = Modifier.size(ERROR_IMAGE_SIZE),
        )
        Spacer(modifier = Modifier.height(WooTheme.spacing.space6))
        Text(
            text = stringResource(message),
            color = WooTheme.colors.surface.onDefault,
            style = WooTheme.text.titleLarge.emphasized,
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailAddPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailPreview(ProductDetailPreviewData.addProductState)
    }
}

@Preview(name = "Write with AI tooltip - RTL", locale = "ar", widthDp = 320, heightDp = 640)
@Composable
private fun ProductDetailTooltipRtlPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailPreview(ProductDetailPreviewData.addProductState)
    }
}

@Preview(name = "Write with AI tooltip - compact", widthDp = 320, heightDp = 480)
@Composable
private fun ProductDetailTooltipCompactPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailPreview(ProductDetailPreviewData.addProductState)
    }
}

@Preview(name = "Write with AI tooltip - compact 2x font", widthDp = 320, heightDp = 480, fontScale = 2f)
@Composable
private fun ProductDetailTooltipCompactLargeFontPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailPreview(ProductDetailPreviewData.addProductState)
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailExistingPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailPreview(ProductDetailPreviewData.existingProductState)
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailWarningPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailPreview(ProductDetailPreviewData.warningState)
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailLoadingPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailPreview(ProductDetailScreenState.Loading)
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailErrorPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailPreview(ProductDetailScreenState.Error(R.string.product_detail_fetch_product_error))
    }
}

@Composable
private fun ProductDetailPreview(state: ProductDetailScreenState) {
    Column(modifier = Modifier.fillMaxSize()) {
        ProductDetailScreen(
            state = state,
            onLinkedProductPromoClicked = {},
            onLinkedProductPromoDismissed = {},
            modifier = Modifier.weight(1f),
        )
        ProductDetailFooter(state = state, onAddMoreClicked = {})
    }
}

private const val LINKED_PROMO_KEY = "linked_product_promo"
private const val CARD_CAPTION_KEY = "caption"
private const val CARD_SPACER_KEY = "spacer"
private const val LOADING_DELAY_MS = 250L
private const val SKELETON_ROW_COUNT = 5
private const val SKELETON_TITLE_WIDTH = 0.55f
private val ADD_MORE_ELEVATION = 4.dp
private val ERROR_IMAGE_SIZE = 160.dp
private val SKELETON_TITLE_HEIGHT = 32.dp
private val SKELETON_ROW_HEIGHT = 72.dp
