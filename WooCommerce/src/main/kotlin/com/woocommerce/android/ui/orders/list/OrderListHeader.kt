package com.woocommerce.android.ui.orders.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooFilledButton
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeader
import com.woocommerce.android.ui.compose.designsystem.component.WooSearchField
import com.woocommerce.android.ui.compose.designsystem.icons.BarcodeScan
import com.woocommerce.android.ui.compose.designsystem.icons.Ellipsis
import com.woocommerce.android.ui.compose.designsystem.icons.MagnifyingGlass
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.compose.designsystem.icons.Xmark
import com.woocommerce.android.util.StringUtils

@Composable
@Suppress("LongParameterList")
internal fun OrderListHeader(
    content: OrderListHeaderContent,
    searchQuery: String,
    filterCount: Int,
    lastUpdate: String?,
    onSearchClicked: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onSearchClosed: () -> Unit,
    onBarcodeClicked: () -> Unit,
    onFiltersClicked: () -> Unit,
    onSelectionCloseClicked: () -> Unit,
    onSelectionUpdateStatusClicked: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(content.mode) {
        if (content.mode == OrderListHeaderMode.Selection) {
            keyboardController?.hide()
        }
    }

    AnimatedContent(
        targetState = content,
        contentKey = OrderListHeaderContent::mode,
        label = "order-list-header-mode",
    ) { headerContent ->
        when (headerContent.mode) {
            OrderListHeaderMode.Selection -> OrderListSelectionHeader(
                selectedOrderCount = headerContent.selectedOrderCount,
                onCloseClicked = onSelectionCloseClicked,
                onUpdateStatusClicked = onSelectionUpdateStatusClicked,
            )
            OrderListHeaderMode.Search -> OrderListSearchHeader(
                query = searchQuery,
                filterCount = filterCount,
                onQueryChanged = onSearchQueryChanged,
                onSearchSubmitted = onSearchSubmitted,
                onSearchClosed = onSearchClosed,
            )
            OrderListHeaderMode.Browsing -> OrderListBrowsingHeader(
                filterCount = filterCount,
                lastUpdate = lastUpdate,
                onSearchClicked = onSearchClicked,
                onBarcodeClicked = onBarcodeClicked,
                onFiltersClicked = onFiltersClicked,
            )
        }
    }
}

@Composable
private fun OrderListBrowsingHeader(
    filterCount: Int,
    lastUpdate: String?,
    onSearchClicked: () -> Unit,
    onBarcodeClicked: () -> Unit,
    onFiltersClicked: () -> Unit,
) {
    Column {
        WooPageHeader(
            title = stringResource(R.string.orders),
            showDivider = false,
            actions = {
                WooOutlinedIconButton(
                    imageVector = WooIcons.Regular.BarcodeScan,
                    contentDescription = stringResource(R.string.scan_barcode),
                    onClick = onBarcodeClicked,
                    modifier = Modifier.testTag(OrderListTestTags.BARCODE_ACTION),
                )
                WooOutlinedIconButton(
                    imageVector = WooIcons.Regular.MagnifyingGlass,
                    contentDescription = stringResource(R.string.orderlist_search_hint),
                    onClick = onSearchClicked,
                    modifier = Modifier.testTag(OrderListTestTags.SEARCH_ACTION),
                )
            },
        )
        OrderListBrowsingControls(
            filterCount = filterCount,
            lastUpdate = lastUpdate,
            onFiltersClicked = onFiltersClicked,
        )
    }
}

@Composable
private fun OrderListSearchHeader(
    query: String,
    filterCount: Int,
    onQueryChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onSearchClosed: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }

    Column {
        WooSearchField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = stringResource(
                if (filterCount > 0) {
                    R.string.orderlist_search_hint_active_filters
                } else {
                    R.string.orderlist_search_hint
                }
            ),
            onClearClick = { onQueryChanged("") },
            clearContentDescription = stringResource(R.string.clear),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearchSubmitted(query)
                    keyboardController?.hide()
                }
            ),
            trailingActionText = stringResource(R.string.cancel),
            onTrailingActionClick = {
                keyboardController?.hide()
                onSearchClosed()
            },
            focusRequester = focusRequester,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(OrderListTestTags.SEARCH_FIELD),
        )
        WooDivider()
    }
}

@Composable
private fun OrderListSelectionHeader(
    selectedOrderCount: Int,
    onCloseClicked: () -> Unit,
    onUpdateStatusClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCountText = StringUtils.getQuantityString(
        quantity = selectedOrderCount,
        default = R.string.orderlist_selection_count,
        one = R.string.orderlist_selection_count_single,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(SELECTION_HEADER_HEIGHT)
            .background(WooTheme.colors.surface.default)
            .testTag(OrderListTestTags.SELECTION_HEADER),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = WooTheme.padding.padding3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooIconButton(
                imageVector = WooIcons.Regular.Xmark,
                contentDescription = stringResource(R.string.close),
                onClick = onCloseClicked,
                modifier = Modifier.testTag(OrderListTestTags.SELECTION_CLOSE),
            )
            Text(
                text = selectedCountText,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = WooTheme.padding.padding3)
                    .semantics {
                        heading()
                        liveRegion = LiveRegionMode.Polite
                    }
                    .testTag(OrderListTestTags.SELECTION_TITLE),
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.titleMedium.strong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OrderSelectionOverflowMenu(onUpdateStatusClicked)
        }
        WooDivider()
    }
}

@Composable
private fun OrderSelectionOverflowMenu(
    onUpdateStatusClicked: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        WooIconButton(
            imageVector = WooIcons.Regular.Ellipsis,
            contentDescription = stringResource(R.string.more_options),
            onClick = { isExpanded = true },
            modifier = Modifier.testTag(OrderListTestTags.SELECTION_OVERFLOW),
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            containerColor = WooTheme.colors.surface.default,
            modifier = Modifier.testTag(OrderListTestTags.SELECTION_MENU),
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.orderlist_selection_menu_update_status),
                        style = WooTheme.text.bodyLarge.regular,
                        color = WooTheme.colors.surface.onDefault,
                    )
                },
                onClick = {
                    isExpanded = false
                    onUpdateStatusClicked()
                },
                modifier = Modifier.testTag(OrderListTestTags.SELECTION_UPDATE_STATUS),
            )
        }
    }
}

@Composable
private fun OrderListBrowsingControls(
    filterCount: Int,
    lastUpdate: String?,
    onFiltersClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(WooTheme.colors.surface.bright)
            .testTag(OrderListTestTags.BROWSING_CONTROLS),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = WooTheme.padding.padding5,
                    vertical = WooTheme.padding.padding3,
                ),
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space1),
            ) {
                Text(
                    text = stringResource(
                        if (filterCount > 0) {
                            R.string.orderfilters_filter_card_title_filtered_orders
                        } else {
                            R.string.orderfilters_filter_card_title_all_orders
                        }
                    ),
                    modifier = Modifier.semantics { heading() },
                    color = WooTheme.colors.surface.onDefault,
                    style = WooTheme.text.titleSmall.strong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                lastUpdate?.takeIf(String::isNotBlank)?.let { value ->
                    Text(
                        text = value,
                        color = colorResource(R.color.color_on_surface_medium),
                        style = WooTheme.text.bodySmall.regular,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (filterCount > 0) {
                WooFilledButton(
                    text = stringResource(R.string.product_list_filters_selected, filterCount),
                    onClick = onFiltersClicked,
                    size = WooButtonSize.Small,
                    modifier = Modifier.testTag(OrderListTestTags.FILTERS),
                )
            } else {
                WooOutlinedButton(
                    text = stringResource(R.string.product_list_filters),
                    onClick = onFiltersClicked,
                    size = WooButtonSize.Small,
                    modifier = Modifier.testTag(OrderListTestTags.FILTERS),
                )
            }
        }
        WooDivider()
    }
}

private val SELECTION_HEADER_HEIGHT = 64.dp
