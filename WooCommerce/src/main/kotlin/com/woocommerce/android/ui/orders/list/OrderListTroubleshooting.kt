package com.woocommerce.android.ui.orders.list

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooButtonSize
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.icons.AngleDown
import com.woocommerce.android.ui.compose.designsystem.icons.AngleUp
import com.woocommerce.android.ui.compose.designsystem.icons.CircleInfo
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

internal data class OrderListTroubleshootingPresentation(
    val type: OrderListTroubleshootingType,
    val isExpanded: Boolean = true,
)

internal enum class OrderListTroubleshootingType {
    ParsingError,
    Timeout,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OrderListTroubleshooting(
    presentation: OrderListTroubleshootingPresentation,
    onExpandedChanged: (Boolean) -> Unit,
    onTroubleshootingClicked: () -> Unit,
    onContactSupportClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(OrderListScreenTestTags.TROUBLESHOOTING),
    ) {
        Surface(
            color = WooTheme.colors.surface.default,
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = presentation.isExpanded,
                            role = Role.Button,
                            onValueChange = onExpandedChanged,
                        )
                        .testTag(OrderListScreenTestTags.TROUBLESHOOTING_TOGGLE)
                        .padding(
                            horizontal = WooTheme.padding.padding7,
                            vertical = WooTheme.padding.padding4,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = WooIcons.Regular.CircleInfo,
                        contentDescription = null,
                        tint = WooTheme.colors.primary,
                        modifier = Modifier.size(WooTheme.iconSize.size24),
                    )
                    Spacer(modifier = Modifier.width(WooTheme.spacing.space4))
                    Text(
                        text = stringResource(presentation.type.titleRes),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() },
                        color = WooTheme.colors.surface.onDefault,
                        style = WooTheme.text.titleMedium.strong,
                    )
                    Spacer(modifier = Modifier.width(WooTheme.spacing.space3))
                    Icon(
                        imageVector = if (presentation.isExpanded) {
                            WooIcons.Regular.AngleUp
                        } else {
                            WooIcons.Regular.AngleDown
                        },
                        contentDescription = null,
                        tint = WooTheme.colors.surface.onVariant,
                        modifier = Modifier.size(WooTheme.iconSize.size18),
                    )
                }
                AnimatedVisibility(visible = presentation.isExpanded) {
                    Column(
                        modifier = Modifier.padding(
                            start = WooTheme.padding.padding7,
                            end = WooTheme.padding.padding7,
                            bottom = WooTheme.padding.padding5,
                        ),
                        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
                    ) {
                        Text(
                            text = stringResource(presentation.type.messageRes),
                            color = WooTheme.colors.surface.onVariant,
                            style = WooTheme.text.bodyMedium.regular,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(
                                WooTheme.spacing.space3,
                                Alignment.End,
                            ),
                            verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space3),
                        ) {
                            WooOutlinedButton(
                                text = stringResource(R.string.error_troubleshooting),
                                onClick = onTroubleshootingClicked,
                                size = WooButtonSize.Small,
                                modifier = Modifier.testTag(
                                    OrderListScreenTestTags.TROUBLESHOOTING_ACTION
                                ),
                            )
                            WooOutlinedButton(
                                text = stringResource(R.string.support_contact),
                                onClick = onContactSupportClicked,
                                size = WooButtonSize.Small,
                                modifier = Modifier.testTag(
                                    OrderListScreenTestTags.CONTACT_SUPPORT_ACTION
                                ),
                            )
                        }
                    }
                }
            }
        }
        WooDivider()
    }
}

@get:StringRes
private val OrderListTroubleshootingType.titleRes: Int
    get() = when (this) {
        OrderListTroubleshootingType.ParsingError -> R.string.orderlist_parsing_error_title
        OrderListTroubleshootingType.Timeout -> R.string.orderlist_timeout_error_title
    }

@get:StringRes
private val OrderListTroubleshootingType.messageRes: Int
    get() = when (this) {
        OrderListTroubleshootingType.ParsingError -> R.string.orderlist_parsing_error_message
        OrderListTroubleshootingType.Timeout -> R.string.orderlist_timeout_error_message
    }
