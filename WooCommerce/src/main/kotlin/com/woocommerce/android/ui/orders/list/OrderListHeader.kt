package com.woocommerce.android.ui.orders.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedIconButton
import com.woocommerce.android.ui.compose.designsystem.component.WooPageHeader
import com.woocommerce.android.ui.compose.designsystem.component.WooSearchField
import com.woocommerce.android.ui.compose.designsystem.icons.BarcodeScan
import com.woocommerce.android.ui.compose.designsystem.icons.MagnifyingGlass
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
@Suppress("LongParameterList")
internal fun OrderListHeader(
    content: OrderListHeaderContent,
    searchQuery: String,
    filterCount: Int,
    showBrowsingControls: Boolean,
    onSearchClicked: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onSearchClosed: () -> Unit,
    onBarcodeClicked: () -> Unit,
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
            OrderListHeaderMode.Selection -> OrderSelectionHeader(
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
                showDivider = !showBrowsingControls,
                onSearchClicked = onSearchClicked,
                onBarcodeClicked = onBarcodeClicked,
            )
        }
    }
}

@Composable
private fun OrderListBrowsingHeader(
    showDivider: Boolean,
    onSearchClicked: () -> Unit,
    onBarcodeClicked: () -> Unit,
) {
    WooPageHeader(
        title = stringResource(R.string.orders),
        showDivider = showDivider,
        actions = {
            WooOutlinedIconButton(
                imageVector = WooIcons.Regular.BarcodeScan,
                contentDescription = stringResource(R.string.scan_barcode),
                onClick = onBarcodeClicked,
                modifier = Modifier.testTag(OrderListScreenTestTags.BARCODE_ACTION),
            )
            WooOutlinedIconButton(
                imageVector = WooIcons.Regular.MagnifyingGlass,
                contentDescription = stringResource(R.string.orderlist_search_hint),
                onClick = onSearchClicked,
                modifier = Modifier.testTag(OrderListScreenTestTags.SEARCH_ACTION),
            )
        },
    )
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
                .testTag(OrderListScreenTestTags.SEARCH_FIELD),
        )
        WooDivider()
    }
}
