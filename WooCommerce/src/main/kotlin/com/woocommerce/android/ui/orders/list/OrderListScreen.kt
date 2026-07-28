package com.woocommerce.android.ui.orders.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.icons.Plus
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
@Suppress("LongParameterList")
internal fun OrderListScreen(
    state: OrderListScreenState,
    orderListContent: @Composable (Modifier) -> Unit,
    onSearchClicked: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchSubmitted: (String) -> Unit,
    onSearchClosed: () -> Unit,
    onBarcodeClicked: () -> Unit,
    onFiltersClicked: () -> Unit,
    onCreateOrderClicked: () -> Unit,
    onSelectionCloseClicked: () -> Unit,
    onSelectionUpdateStatusClicked: () -> Unit,
    onTroubleshootingExpandedChanged: (Boolean) -> Unit,
    onTroubleshootingClicked: () -> Unit,
    onContactSupportClicked: () -> Unit,
    modifier: Modifier = Modifier,
    jitmContent: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag(OrderListScreenTestTags.SCREEN),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OrderListHeader(
                content = state.headerContent,
                searchQuery = state.searchQuery,
                filterCount = state.filterCount,
                showBrowsingControls = state.shouldShowBrowsingControls,
                onSearchClicked = onSearchClicked,
                onSearchQueryChanged = onSearchQueryChanged,
                onSearchSubmitted = onSearchSubmitted,
                onSearchClosed = onSearchClosed,
                onBarcodeClicked = onBarcodeClicked,
                onSelectionCloseClicked = onSelectionCloseClicked,
                onSelectionUpdateStatusClicked = onSelectionUpdateStatusClicked,
            )

            state.troubleshooting?.let { presentation ->
                OrderListTroubleshooting(
                    presentation = presentation,
                    onExpandedChanged = onTroubleshootingExpandedChanged,
                    onTroubleshootingClicked = onTroubleshootingClicked,
                    onContactSupportClicked = onContactSupportClicked,
                )
            }

            AnimatedVisibility(visible = state.shouldShowBrowsingControls) {
                OrderListBrowsingControls(
                    filterCount = state.filterCount,
                    lastUpdate = state.lastUpdate,
                    onFiltersClicked = onFiltersClicked,
                )
            }

            jitmContent?.let { content ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(OrderListScreenTestTags.JITM),
                ) {
                    content()
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag(OrderListScreenTestTags.BODY),
            ) {
                orderListContent(Modifier.fillMaxSize())
            }
        }

        AnimatedVisibility(
            visible = state.shouldShowCreateOrderFab,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(WooTheme.padding.padding5),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            FloatingActionButton(
                onClick = onCreateOrderClicked,
                containerColor = WooTheme.colors.primary,
                contentColor = WooTheme.colors.onPrimary,
                modifier = Modifier.testTag(OrderListScreenTestTags.CREATE_ORDER_FAB),
            ) {
                Icon(
                    imageVector = WooIcons.Regular.Plus,
                    contentDescription = stringResource(R.string.orderlist_create_order_button_description),
                    modifier = Modifier.size(WooTheme.iconSize.size24),
                )
            }
        }
    }
}
