package com.woocommerce.android.ui.products.details

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.AngleLeft
import com.woocommerce.android.ui.compose.designsystem.icons.Ellipsis
import com.woocommerce.android.ui.compose.designsystem.icons.Plus
import com.woocommerce.android.ui.compose.designsystem.icons.Share
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.compose.designsystem.icons.Xmark
import kotlinx.coroutines.delay

@Composable
fun ProductDetailScreen(
    state: ProductDetailPageUiState,
    callbacks: ProductDetailPageCallbacks,
    modifier: Modifier = Modifier,
) {
    val contentListState = rememberLazyListState()
    val galleryListState = rememberLazyListState()
    val showAddMore = (state.screen as? ProductDetailScreenState.Content)?.showAddMore == true
    Surface(
        modifier = modifier.fillMaxSize(),
        color = WooTheme.colors.background.section,
    ) {
        Column(modifier = Modifier.testTag(ProductDetailTestTags.PAGE)) {
            ProductDetailTopAppBar(
                title = state.title,
                state = state.topAppBar,
                callbacks = callbacks.topAppBar,
            )
            Box(modifier = Modifier.weight(1F)) {
                ProductDetailPageBody(
                    state = state,
                    callbacks = callbacks,
                    contentListState = contentListState,
                    galleryListState = galleryListState,
                )
            }
            ProductDetailFooter(
                showAddMore = showAddMore,
                onAddMoreClicked = callbacks.content.onAddMoreClicked,
            )
        }
    }
}

@Composable
private fun ProductDetailPageBody(
    state: ProductDetailPageUiState,
    callbacks: ProductDetailPageCallbacks,
    contentListState: LazyListState,
    galleryListState: LazyListState,
) {
    when (val screen = state.screen) {
        ProductDetailScreenState.Loading -> ProductDetailLoadingPage(
            imageState = state.image,
            imageCallbacks = callbacks.image,
            galleryListState = galleryListState,
            showUploadError = state.showUploadError,
            onUploadErrorClicked = callbacks.onUploadErrorClicked,
        )
        is ProductDetailScreenState.Empty -> screen.message?.let { ProductDetailError(it) } ?: ProductDetailEmpty()
        is ProductDetailScreenState.Error -> ProductDetailError(screen.message)
        is ProductDetailScreenState.Content -> ProductDetailContent(
            state = screen,
            imageState = state.image,
            callbacks = callbacks,
            contentListState = contentListState,
            galleryListState = galleryListState,
            showUploadError = state.showUploadError,
        )
    }
}

@Composable
private fun ProductDetailContent(
    state: ProductDetailScreenState.Content,
    imageState: ProductDetailImageUiState,
    callbacks: ProductDetailPageCallbacks,
    contentListState: LazyListState,
    galleryListState: LazyListState,
    showUploadError: Boolean,
) {
    LazyColumn(
        state = contentListState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(ProductDetailTestTags.LIST),
    ) {
        item(key = IMAGE_HEADER_KEY, contentType = IMAGE_HEADER_CONTENT_TYPE) {
            ProductDetailImageSection(
                state = imageState,
                callbacks = callbacks.image,
                galleryListState = galleryListState,
                parallaxTranslation = {
                    headerParallaxTranslation(
                        firstVisibleItemIndex = contentListState.firstVisibleItemIndex,
                        firstVisibleItemScrollOffset = contentListState.firstVisibleItemScrollOffset,
                    )
                },
            )
        }
        if (showUploadError) {
            stickyHeader(key = UPLOAD_ERROR_KEY) {
                ProductDetailUploadError(
                    isVisible = true,
                    onClick = callbacks.onUploadErrorClicked,
                )
            }
        }
        if (state.showLinkedProductPromo) {
            item(key = LINKED_PROMO_KEY) {
                LinkedProductPromo(
                    onClick = callbacks.content.onLinkedProductPromoClicked,
                    onDismiss = callbacks.content.onLinkedProductPromoDismissed,
                )
            }
        }
        state.cards.forEach { card ->
            productDetailCard(card)
        }
    }
}

@Composable
private fun ProductDetailFooter(
    showAddMore: Boolean,
    onAddMoreClicked: () -> Unit,
) {
    if (showAddMore) {
        ProductDetailAddMore(
            onClick = onAddMoreClicked,
            modifier = Modifier.testTag(ProductDetailTestTags.FOOTER),
        )
    }
}

@Composable
private fun ProductDetailEmpty() {
    Box(modifier = Modifier.fillMaxSize())
}

@Composable
private fun ProductDetailLoadingPage(
    imageState: ProductDetailImageUiState,
    imageCallbacks: ProductDetailImageCallbacks,
    galleryListState: LazyListState,
    showUploadError: Boolean,
    onUploadErrorClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ProductDetailImageSection(
            state = imageState,
            callbacks = imageCallbacks,
            galleryListState = galleryListState,
        )
        ProductDetailUploadError(
            isVisible = showUploadError,
            onClick = onUploadErrorClicked,
        )
        ProductDetailLoading()
    }
}

@Composable
private fun ProductDetailImageSection(
    state: ProductDetailImageUiState,
    callbacks: ProductDetailImageCallbacks,
    galleryListState: LazyListState,
    parallaxTranslation: () -> Float = { 0F },
) {
    if (state == ProductDetailImageUiState.Hidden) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .background(WooTheme.colors.surface.bright),
    ) {
        ProductDetailImageHeader(
            state = state,
            listState = galleryListState,
            onImageClicked = callbacks.onImageClicked,
            onAddImageClicked = callbacks.onAddImageClicked,
            onImagesUnavailableClicked = callbacks.onImagesUnavailableClicked,
            modifier = Modifier
                .graphicsLayer { translationY = parallaxTranslation() }
                .padding(
                    horizontal = WooTheme.spacing.space3,
                    vertical = WooTheme.spacing.space5,
                ),
        )
        WooDivider()
    }
}

private fun headerParallaxTranslation(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
): Float = if (firstVisibleItemIndex == 0) {
    firstVisibleItemScrollOffset * HEADER_PARALLAX_MULTIPLIER
} else {
    0F
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

private fun productDetailItemKey(cardKey: String, itemKey: String) = "$cardKey:$itemKey"

@Composable
private fun ProductDetailTopAppBar(
    title: String,
    state: ProductDetailTopAppBarUiState,
    callbacks: ProductDetailTopAppBarCallbacks,
) {
    val navigationIcon = when (state.navigation) {
        ProductDetailTopAppBarNavigation.BACK -> WooIcons.Regular.AngleLeft
        ProductDetailTopAppBarNavigation.CLOSE -> WooIcons.Regular.Xmark
        null -> null
    }
    val navigationDescription = when (state.navigation) {
        ProductDetailTopAppBarNavigation.BACK -> stringResource(R.string.back)
        ProductDetailTopAppBarNavigation.CLOSE -> stringResource(R.string.close)
        null -> null
    }
    WooTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        navigationIconContentDescription = navigationDescription,
        onNavigationClick = callbacks.onNavigationClicked.takeIf { state.navigation != null },
        windowInsets = WindowInsets(0),
        modifier = Modifier.testTag(ProductDetailTestTags.TOP_APP_BAR),
        actions = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                state.primaryAction?.let { action ->
                    ProductDetailTopAppBarTextAction(
                        action = action,
                        onClick = { callbacks.onActionClicked(action) },
                    )
                }
                state.shareAction?.let { action ->
                    WooOutlinedIconButton(
                        imageVector = WooIcons.Regular.Share,
                        contentDescription = stringResource(action.label),
                        onClick = { callbacks.onActionClicked(action) },
                    )
                }
                if (state.overflowActions.isNotEmpty()) {
                    ProductDetailTopAppBarOverflow(
                        actions = state.overflowActions,
                        onActionClicked = callbacks.onActionClicked,
                    )
                }
            }
        },
    )
}

@Composable
private fun ProductDetailTopAppBarTextAction(
    action: ProductDetailTopAppBarAction,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = WooTheme.colors.primary),
        modifier = Modifier.widthIn(max = TOP_APP_BAR_ACTION_TEXT_MAX_WIDTH),
    ) {
        Text(
            text = stringResource(action.label),
            style = WooTheme.text.labelLarge.emphasized,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ProductDetailTopAppBarOverflow(
    actions: List<ProductDetailTopAppBarAction>,
    onActionClicked: (ProductDetailTopAppBarAction) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        WooOutlinedIconButton(
            imageVector = WooIcons.Regular.Ellipsis,
            contentDescription = stringResource(R.string.more_options),
            onClick = { isExpanded = true },
            modifier = Modifier.testTag(ProductDetailTestTags.TOP_APP_BAR_OVERFLOW),
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            containerColor = WooTheme.colors.surface.default,
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(action.label),
                            color = if (action.isDestructive) {
                                WooTheme.colors.error
                            } else {
                                WooTheme.colors.surface.onDefault
                            },
                            style = WooTheme.text.bodyLarge.regular,
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onActionClicked(action)
                    },
                )
            }
        }
    }
}

@Composable
private fun ProductDetailAddMore(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = WooTheme.colors.surface.bright,
        shadowElevation = ADD_MORE_ELEVATION,
        modifier = modifier.fillMaxWidth(),
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
            .background(WooTheme.colors.surface.bright)
            .padding(WooTheme.padding.padding5),
        color = WooTheme.colors.container.secondaryContainer,
        contentColor = WooTheme.colors.container.onSecondaryContainer,
        shape = RoundedCornerShape(WooTheme.radius.large),
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
    val inspectionMode = LocalInspectionMode.current
    var isVisible by remember { mutableStateOf(inspectionMode) }
    LaunchedEffect(inspectionMode) {
        if (!inspectionMode) {
            delay(LOADING_DELAY_MS)
            isVisible = true
        }
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
                    .clip(RoundedCornerShape(WooTheme.radius.medium)),
            )
            repeat(SKELETON_ROW_COUNT) {
                SkeletonView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SKELETON_ROW_HEIGHT)
                        .clip(RoundedCornerShape(WooTheme.radius.medium)),
                )
            }
        }
    }
}

@Composable
private fun ProductDetailError(@StringRes message: Int) {
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
    ProductDetailPreview(
        pageState(
            title = "Add product",
            screen = ProductDetailPreviewData.addProductState,
            image = ProductDetailImageUiState.AddImage,
            topAppBar = previewTopAppBar(primary = ProductDetailTopAppBarAction.PUBLISH),
        )
    )
}

@Preview(name = "Write with AI tooltip - RTL", locale = "ar", widthDp = 320, heightDp = 640)
@Composable
private fun ProductDetailTooltipRtlPreview() {
    ProductDetailAddPreview()
}

@Preview(name = "Write with AI tooltip - compact", widthDp = 320, heightDp = 480)
@Composable
private fun ProductDetailTooltipCompactPreview() {
    ProductDetailAddPreview()
}

@Preview(name = "Write with AI tooltip - compact 2x font", widthDp = 320, heightDp = 480, fontScale = 2f)
@Composable
private fun ProductDetailTooltipCompactLargeFontPreview() {
    ProductDetailAddPreview()
}

@PreviewLightDark
@Composable
private fun ProductDetailExistingPreview() {
    ProductDetailPreview(
        pageState(
            title = "Beanie",
            screen = ProductDetailPreviewData.existingProductState,
            image = previewGalleryState(),
            topAppBar = previewTopAppBar(
                primary = ProductDetailTopAppBarAction.SAVE,
                overflow = FULL_PREVIEW_OVERFLOW,
            ),
            showUploadError = true,
        )
    )
}

@PreviewLightDark
@Composable
private fun ProductDetailWarningPreview() {
    ProductDetailPreview(
        pageState(
            title = "Variable product",
            screen = ProductDetailPreviewData.warningState,
            image = ProductDetailImageUiState.Unavailable,
            topAppBar = previewTopAppBar(share = true),
        )
    )
}

@PreviewLightDark
@Composable
private fun ProductDetailLoadingPreview() {
    ProductDetailPreview(
        pageState(
            title = "Product",
            screen = ProductDetailScreenState.Loading,
            image = ProductDetailImageUiState.Loading,
            topAppBar = previewTopAppBar(),
        )
    )
}

@PreviewLightDark
@Composable
private fun ProductDetailErrorPreview() {
    ProductDetailPreview(
        pageState(
            title = "Product",
            screen = ProductDetailScreenState.Error(R.string.product_detail_fetch_product_error),
            image = ProductDetailImageUiState.Hidden,
            topAppBar = previewTopAppBar(),
        )
    )
}

@Preview(name = "Large font", fontScale = 2F, widthDp = 360, heightDp = 800)
@Composable
private fun ProductDetailLargeFontPreview() {
    ProductDetailAddPreview()
}

@Preview(name = "RTL", locale = "ar", widthDp = 360, heightDp = 800)
@Composable
private fun ProductDetailRtlPreview() {
    ProductDetailExistingPreview()
}

@Preview(name = "Two pane", widthDp = 840, heightDp = 900)
@Composable
private fun ProductDetailTwoPanePreview() {
    ProductDetailPreview(
        pageState(
            title = "Beanie",
            screen = ProductDetailPreviewData.existingProductState,
            image = previewGalleryState(),
            topAppBar = previewTopAppBar(navigation = null, share = true),
        )
    )
}

@Composable
private fun ProductDetailPreview(state: ProductDetailPageUiState) {
    WooDesignSystemThemeWithBackground {
        ProductDetailScreen(state = state, callbacks = PREVIEW_CALLBACKS)
    }
}

@Composable
private fun previewGalleryState(): ProductDetailImageUiState {
    val context = LocalContext.current
    val source = "android.resource://${context.packageName}/${R.drawable.blaze_campaign_product_placeholder}"
    return ProductDetailImageUiState.Gallery(
        listOf(
            ProductDetailImageUiItem.Persisted("preview_cover", source, true),
            ProductDetailImageUiItem.Persisted("preview_second", source, false),
            ProductDetailImageUiItem.Add,
        )
    )
}

private fun pageState(
    title: String,
    screen: ProductDetailScreenState,
    image: ProductDetailImageUiState,
    topAppBar: ProductDetailTopAppBarUiState,
    showUploadError: Boolean = false,
) = ProductDetailPageUiState(title, topAppBar, screen, image, showUploadError)

private fun previewTopAppBar(
    navigation: ProductDetailTopAppBarNavigation? = ProductDetailTopAppBarNavigation.BACK,
    primary: ProductDetailTopAppBarAction? = null,
    share: Boolean = false,
    overflow: List<ProductDetailTopAppBarAction> = listOf(ProductDetailTopAppBarAction.SETTINGS),
) = ProductDetailTopAppBarUiState(
    navigation = navigation,
    primaryAction = primary,
    shareAction = ProductDetailTopAppBarAction.SHARE.takeIf { share },
    overflowActions = overflow,
)

private const val LINKED_PROMO_KEY = "linked_product_promo"
private const val IMAGE_HEADER_KEY = "product_detail_image_header"
private const val IMAGE_HEADER_CONTENT_TYPE = "product_detail_image_header"
private const val UPLOAD_ERROR_KEY = "product_detail_upload_error"
private const val CARD_CAPTION_KEY = "caption"
private const val CARD_SPACER_KEY = "spacer"
private const val LOADING_DELAY_MS = 250L
private const val SKELETON_ROW_COUNT = 5
private const val SKELETON_TITLE_WIDTH = 0.55f

// Matches CollapsingToolbarLayout's default parallax multiplier from the legacy Product Detail screen.
private const val HEADER_PARALLAX_MULTIPLIER = 0.5F
private val ADD_MORE_ELEVATION = 4.dp
private val ERROR_IMAGE_SIZE = 160.dp
private val SKELETON_TITLE_HEIGHT = 32.dp
private val SKELETON_ROW_HEIGHT = 72.dp
private val TOP_APP_BAR_ACTION_TEXT_MAX_WIDTH = 136.dp

private val FULL_PREVIEW_OVERFLOW = listOf(
    ProductDetailTopAppBarAction.PUBLISH,
    ProductDetailTopAppBarAction.SAVE_AS_DRAFT,
    ProductDetailTopAppBarAction.SHARE,
    ProductDetailTopAppBarAction.VIEW_PRODUCT,
    ProductDetailTopAppBarAction.SETTINGS,
    ProductDetailTopAppBarAction.DUPLICATE,
    ProductDetailTopAppBarAction.TRASH,
)

private val PREVIEW_CALLBACKS = ProductDetailPageCallbacks(
    topAppBar = ProductDetailTopAppBarCallbacks(onNavigationClicked = {}, onActionClicked = { _ -> }),
    image = ProductDetailImageCallbacks(
        onImageClicked = {},
        onAddImageClicked = {},
        onImagesUnavailableClicked = {},
    ),
    content = ProductDetailContentCallbacks(
        onLinkedProductPromoClicked = {},
        onLinkedProductPromoDismissed = {},
        onAddMoreClicked = {},
    ),
    onUploadErrorClicked = {},
)
