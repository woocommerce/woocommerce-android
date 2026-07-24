package com.woocommerce.android.ui.products.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.requestFocus
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooCircularProgressIndicator
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledButton
import com.woocommerce.android.ui.compose.designsystem.icons.CheckSmall
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
internal fun ProductListContent(
    state: ProductListScreenState,
    listState: LazyListState,
    showAddProductFab: Boolean,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onEmptyAddProductClicked: () -> Unit,
    onProductTapped: (Long) -> Unit,
    onProductLongPressed: (Long) -> Unit,
    onProductSelectionToggled: (Long) -> Unit,
) {
    WCPullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        enabled = state.isPullToRefreshEnabled && !state.isSearchActive && !state.isSelecting,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isSkeletonShown -> ProductListSkeleton()
            state.isEmptyViewVisible -> ProductListEmptyState(
                isSearchActive = state.isSearchActive,
                isFiltered = state.filterCount > 0,
                searchQuery = state.searchQuery,
                onAddProductClicked = onEmptyAddProductClicked,
            )
            else -> ProductLazyList(
                products = state.products,
                selectedProductIds = state.selectedProductIds,
                uploadingProductIds = state.uploadingProductIds,
                highlightedProductId = state.highlightedProductId,
                isLoadInProgress = state.isLoading,
                isLoadingMore = state.isLoadingMore,
                canLoadMore = state.canLoadMore,
                bottomContentPadding = if (showAddProductFab) {
                    FAB_SIZE + WooTheme.padding.padding5 + WooTheme.padding.padding5
                } else {
                    WooTheme.padding.padding5
                },
                listState = listState,
                onLoadMore = onLoadMore,
                onProductTapped = onProductTapped,
                onProductLongPressed = onProductLongPressed,
                onProductSelectionToggled = onProductSelectionToggled,
            )
        }
    }
}

@Composable
private fun ProductLazyList(
    products: List<ProductListItemUiModel>,
    selectedProductIds: Set<Long>,
    uploadingProductIds: Set<Long>,
    highlightedProductId: Long?,
    isLoadInProgress: Boolean,
    isLoadingMore: Boolean,
    canLoadMore: Boolean,
    bottomContentPadding: Dp,
    listState: LazyListState,
    onLoadMore: () -> Unit,
    onProductTapped: (Long) -> Unit,
    onProductLongPressed: (Long) -> Unit,
    onProductSelectionToggled: (Long) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(ProductListTestTags.LIST)
            .semantics { collectionInfo = CollectionInfo(products.size, COLLECTION_COLUMN_COUNT) },
        contentPadding = PaddingValues(bottom = bottomContentPadding),
    ) {
        items(items = products, key = ProductListItemUiModel::remoteId) { product ->
            val isSelected = product.remoteId in selectedProductIds
            val isUploadingMedia = product.remoteId in uploadingProductIds
            val isHighlighted = product.remoteId == highlightedProductId
            ProductListItem(
                product = product,
                isSelected = isSelected,
                isUploadingMedia = isUploadingMedia,
                isHighlighted = isHighlighted,
                onClick = { onProductTapped(product.remoteId) },
                onLongClick = { onProductLongPressed(product.remoteId) },
                onToggleSelection = { onProductSelectionToggled(product.remoteId) },
            )
            WooDivider(
                modifier = Modifier
                    .padding(horizontal = WooTheme.padding.padding7)
                    .testTag(ProductListTestTags.productDivider(product.remoteId))
            )
        }
        if (isLoadingMore) {
            item(key = "append-progress") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(ProductListTestTags.APPEND_PROGRESS)
                        .padding(WooTheme.padding.padding5),
                    contentAlignment = Alignment.Center,
                ) {
                    WooCircularProgressIndicator(modifier = Modifier.size(APPEND_PROGRESS_SIZE))
                }
            }
        }
    }
    if (products.isNotEmpty() && !isLoadInProgress && canLoadMore) {
        InfiniteListHandler(
            listState = listState,
            buffer = END_OF_LIST_BUFFER,
            onLoadMore = onLoadMore,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductListItem(
    product: ProductListItemUiModel,
    isSelected: Boolean,
    isUploadingMedia: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelection: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val selectionAction = if (isSelected) {
        stringResource(R.string.product_list_deselect_product, product.name)
    } else {
        stringResource(R.string.product_list_select_product, product.name)
    }
    val background = when {
        isSelected || isHighlighted -> colorResource(R.color.color_item_selected)
        else -> WooTheme.colors.surface.default
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = PRODUCT_ROW_MIN_HEIGHT)
            .testTag(ProductListTestTags.productRow(product.remoteId))
            .background(background)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.DirectionCenter -> {
                        onClick()
                        true
                    }
                    Key.Spacebar -> {
                        onToggleSelection()
                        true
                    }
                    else -> false
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {
                selected = isSelected
                role = Role.Button
                requestFocus {
                    focusRequester.requestFocus()
                    true
                }
                customActions = listOf(
                    CustomAccessibilityAction(selectionAction) {
                        onToggleSelection()
                        true
                    }
                )
            }
            .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductListThumbnail(
            product = product,
            isSelected = isSelected,
            isUploadingMedia = isUploadingMedia,
        )
        Spacer(modifier = Modifier.width(WooTheme.spacing.space5))
        Column(
            modifier = Modifier
                .weight(1f)
                .testTag(ProductListTestTags.productContent(product.remoteId)),
            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        ) {
            Text(
                text = product.name,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.emphasized,
                modifier = Modifier.testTag(ProductListTestTags.productTitle(product.remoteId)),
                maxLines = PRODUCT_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            ProductListMetadata(product)
        }
    }
}

@Composable
private fun ProductListMetadata(product: ProductListItemUiModel) {
    val statusColor = colorResource(
        if (product.isStatusPending) {
            R.color.product_status_fg_pending
        } else {
            R.color.product_status_fg_other
        }
    )
    val metadata = buildAnnotatedString {
        product.status?.let { status ->
            withStyle(SpanStyle(color = statusColor)) { append(status) }
            append(METADATA_SEPARATOR)
        }
        append(product.stockAndPrice)
        product.sku?.let { sku ->
            append(METADATA_SEPARATOR)
            append(sku)
        }
    }

    Text(
        text = metadata,
        color = WooTheme.colors.surface.onVariant,
        style = WooTheme.text.labelMedium.regular,
        modifier = Modifier.testTag(ProductListTestTags.productMetadata(product.remoteId)),
    )
}

@Composable
private fun ProductListThumbnail(
    product: ProductListItemUiModel,
    isSelected: Boolean,
    isUploadingMedia: Boolean,
) {
    Box(
        modifier = Modifier
            .size(PRODUCT_THUMBNAIL_SIZE)
            .testTag(ProductListTestTags.productThumbnail(product.remoteId)),
        contentAlignment = Alignment.Center,
    ) {
        ProductThumbnail(
            imageUrl = product.imageUrl,
            imageSize = PRODUCT_THUMBNAIL_SIZE,
            contentDescription = stringResource(R.string.product_image_content_description),
            cornerRadius = WooTheme.radius.medium,
            modifier = Modifier.alpha(
                if (isUploadingMedia && !isSelected) UPLOADING_THUMBNAIL_ALPHA else 1f
            ),
        )
        when {
            isSelected -> Box(
                modifier = Modifier
                    .matchParentSize()
                    .testTag(ProductListTestTags.selectionIndicator(product.remoteId))
                    .background(
                        color = WooTheme.colors.primary,
                        shape = RoundedCornerShape(WooTheme.radius.medium),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = WooIcons.Regular.CheckSmall,
                    contentDescription = null,
                    tint = WooTheme.colors.onPrimary,
                    modifier = Modifier.size(SELECTION_CHECK_SIZE),
                )
            }
            isUploadingMedia -> WooCircularProgressIndicator(
                modifier = Modifier.size(APPEND_PROGRESS_SIZE)
            )
        }
    }
}

@Composable
private fun ProductListSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ProductListTestTags.SKELETON)
    ) {
        repeat(SKELETON_ITEM_COUNT) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PRODUCT_ROW_MIN_HEIGHT)
                    .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding4),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonView(width = PRODUCT_THUMBNAIL_SIZE, height = PRODUCT_THUMBNAIL_SIZE)
                Spacer(modifier = Modifier.width(WooTheme.spacing.space5))
                Column(
                    verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4)
                ) {
                    SkeletonView(width = SKELETON_TITLE_WIDTH, height = SKELETON_TITLE_HEIGHT)
                    SkeletonView(width = SKELETON_METADATA_WIDTH, height = SKELETON_METADATA_HEIGHT)
                }
            }
        }
    }
}

@Composable
private fun ProductListEmptyState(
    isSearchActive: Boolean,
    isFiltered: Boolean,
    searchQuery: String,
    onAddProductClicked: () -> Unit,
) {
    val title = when {
        isSearchActive -> stringResource(R.string.empty_message_with_search, searchQuery)
        isFiltered -> stringResource(R.string.product_list_empty_filters)
        else -> stringResource(R.string.product_list_empty)
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag(ProductListTestTags.EMPTY)
    ) {
        val showImage = maxHeight >= MIN_EMPTY_STATE_IMAGE_HEIGHT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WooTheme.padding.padding7, vertical = WooTheme.padding.padding8),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.titleMedium.strong,
                textAlign = TextAlign.Center,
            )
            if (showImage) {
                Spacer(modifier = Modifier.height(WooTheme.spacing.space8))
                Image(
                    painter = painterResource(
                        if (isSearchActive) R.drawable.img_empty_search else R.drawable.img_empty_products
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(EMPTY_STATE_IMAGE_SIZE),
                )
            }
            if (!isSearchActive && !isFiltered) {
                Spacer(modifier = Modifier.height(WooTheme.spacing.space8))
                Text(
                    text = stringResource(R.string.empty_product_message),
                    color = WooTheme.colors.surface.onVariant,
                    style = WooTheme.text.bodyMedium.regular,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(WooTheme.spacing.space8))
                WooFilledButton(
                    text = stringResource(R.string.empty_product_add_product_button),
                    onClick = onAddProductClicked,
                    modifier = Modifier.testTag(ProductListTestTags.ADD_ACTION),
                )
            }
        }
    }
}

private const val COLLECTION_COLUMN_COUNT = 1
private const val END_OF_LIST_BUFFER = 3
private const val PRODUCT_TITLE_MAX_LINES = 2
private const val SKELETON_ITEM_COUNT = 8
private const val UPLOADING_THUMBNAIL_ALPHA = 0.3f
private const val METADATA_SEPARATOR = " • "
private val PRODUCT_THUMBNAIL_SIZE = 44.dp
private val PRODUCT_ROW_MIN_HEIGHT = 92.dp
private val FAB_SIZE = 56.dp
private val APPEND_PROGRESS_SIZE = 24.dp
private val SELECTION_CHECK_SIZE = 24.dp
private val EMPTY_STATE_IMAGE_SIZE = 160.dp
private val MIN_EMPTY_STATE_IMAGE_HEIGHT = 400.dp
private val SKELETON_TITLE_WIDTH = 220.dp
private val SKELETON_TITLE_HEIGHT = 16.dp
private val SKELETON_METADATA_WIDTH = 150.dp
private val SKELETON_METADATA_HEIGHT = 14.dp
