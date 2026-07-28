package com.woocommerce.android.ui.orders.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButton
import com.woocommerce.android.ui.compose.designsystem.icons.Ellipsis
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.compose.designsystem.icons.Xmark
import com.woocommerce.android.util.StringUtils

@Composable
internal fun OrderSelectionHeader(
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
            .testTag(OrderListScreenTestTags.SELECTION_HEADER),
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
                modifier = Modifier.testTag(OrderListScreenTestTags.SELECTION_CLOSE),
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
                    .testTag(OrderListScreenTestTags.SELECTION_TITLE),
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
            modifier = Modifier.testTag(OrderListScreenTestTags.SELECTION_OVERFLOW),
        )
        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            containerColor = WooTheme.colors.surface.default,
            modifier = Modifier.testTag(OrderListScreenTestTags.SELECTION_MENU),
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
                modifier = Modifier.testTag(OrderListScreenTestTags.SELECTION_UPDATE_STATUS),
            )
        }
    }
}

private val SELECTION_HEADER_HEIGHT = 64.dp
