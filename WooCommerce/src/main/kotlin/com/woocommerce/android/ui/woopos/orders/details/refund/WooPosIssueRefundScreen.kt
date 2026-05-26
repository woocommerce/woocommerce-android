package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCustomAmountTileAvatar
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosItemImage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmark
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveIconSize
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import java.math.BigDecimal

private fun <S> AnimatedContentTransitionScope<S>.refundFadeTransition(): ContentTransform =
    fadeIn(
        animationSpec = tween(
            durationMillis = 250,
            delayMillis = 200,
            easing = FastOutSlowInEasing
        )
    ) togetherWith fadeOut(
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        )
    ) using null

@Composable
fun WooPosIssueRefundScreen(
    orderId: Long,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    refundReasonUpdate: String? = null,
    disablePartialRefund: Boolean = false,
) {
    val viewModel: WooPosRefundViewModel =
        hiltViewModel<WooPosRefundViewModel, WooPosRefundViewModel.Factory> { factory ->
            factory.create(orderId)
        }

    LaunchedEffect(Unit) {
        viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowOpened)
    }

    refundReasonUpdate?.let { reason ->
        LaunchedEffect(reason) {
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundReasonChanged(reason))
        }
    }

    val handleDismiss = {
        if (viewModel.onDismissRequest()) {
            viewModel.onUIEvent(WooPosRefundUIEvent.RefundFlowDismissed)
            onNavigationEvent(
                WooPosNavigationEvent.GoBackWithResult(
                    key = ISSUE_REFUND_DISMISSED_KEY,
                    value = true
                )
            )
        }
    }

    BackHandler {
        handleDismiss()
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    val toolbarTitle = resolveToolbarTitle(state)

    val currentState = state
    val isProcessing = currentState is WooPosRefundState.Content &&
        currentState.step.isNonCancelable()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        RefundScreenHeader(
            title = toolbarTitle,
            onCloseClicked = { handleDismiss() },
            closeButtonEnabled = !isProcessing,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = WooPosSpacing.Medium.value,
                    end = WooPosSpacing.Medium.value,
                    bottom = WooPosSpacing.XLarge.value,
                ),
        ) {
            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                modifier = Modifier.weight(1f),
                transitionSpec = { refundFadeTransition() },
                label = "refund_state_transition",
            ) { animatedState ->
                when (animatedState) {
                    is WooPosRefundState.Loading -> SelectItemsContent(
                        state = animatedState,
                        onEvent = {},
                    )
                    is WooPosRefundState.Content -> ContentStateHandler(
                        state = animatedState,
                        orderId = orderId,
                        onNavigationEvent = onNavigationEvent,
                        onEvent = viewModel::onUIEvent,
                        disablePartialRefund = disablePartialRefund,
                    )
                    is WooPosRefundState.Error -> ErrorContent(
                        animatedState,
                    )
                    is WooPosRefundState.NoRefundableItems -> NoItemsContent()
                    is WooPosRefundState.RefundSuccess -> RefundSuccessContent(
                        state = animatedState,
                    )
                }
            }

            RefundScreenButtons(
                state = state,
                onDismiss = handleDismiss,
                onEvent = viewModel::onUIEvent,
                onNavigationEvent = onNavigationEvent,
                disablePartialRefund = disablePartialRefund,
            )
        }
    }
}

@Composable
private fun resolveToolbarTitle(state: WooPosRefundState): String {
    return when (state) {
        is WooPosRefundState.Loading ->
            stringResource(R.string.woopos_orders_select_items_to_refund)
        is WooPosRefundState.Content -> when (state.step) {
            WooPosRefundState.Content.RefundStep.SelectItems ->
                stringResource(R.string.woopos_orders_select_items_to_refund)
            WooPosRefundState.Content.RefundStep.ReviewRefund ->
                stringResource(R.string.woopos_orders_review_refund)
            WooPosRefundState.Content.RefundStep.ConfirmRefund,
            WooPosRefundState.Content.RefundStep.Processing,
            WooPosRefundState.Content.RefundStep.PreparingReader,
            WooPosRefundState.Content.RefundStep.ReaderDisconnected,
            is WooPosRefundState.Content.RefundStep.ReadyForRefund,
            WooPosRefundState.Content.RefundStep.ProcessingRefund,
            WooPosRefundState.Content.RefundStep.NotifyingStore ->
                stringResource(R.string.woopos_orders_confirm_refund_title, state.formattedTotal)
        }
        is WooPosRefundState.Error,
        is WooPosRefundState.NoRefundableItems ->
            stringResource(R.string.orderdetail_issue_refund_button)
        is WooPosRefundState.RefundSuccess ->
            stringResource(R.string.woopos_orders_refund_complete)
    }
}

@Composable
private fun RefundScreenHeader(
    title: String,
    onCloseClicked: () -> Unit,
    modifier: Modifier = Modifier,
    closeButtonEnabled: Boolean = true,
) {
    val closeContentDescription = stringResource(R.string.close)
    val isPhone = currentWooPosBreakpoint() == WooPosBreakpoint.Phone
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WooPosComponentSize.XSmall.value),
    ) {
        IconButton(
            onClick = onCloseClicked,
            enabled = closeButtonEnabled,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = WooPosSpacing.Small.value)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_close_24dp),
                contentDescription = closeContentDescription,
                modifier = Modifier.size(WooPosIconSize.Large.value),
            )
        }

        WooPosText(
            text = title,
            style = if (isPhone) WooPosTypography.BodyLarge else WooPosTypography.Heading,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = if (isPhone) 2 else 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = WooPosComponentSize.XSmall.value)
        )
    }
}

@Composable
@Suppress("CyclomaticComplexMethod")
private fun RefundScreenButtons(
    state: WooPosRefundState,
    onDismiss: () -> Unit,
    onEvent: (WooPosRefundUIEvent) -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    disablePartialRefund: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        when (state) {
            is WooPosRefundState.Loading -> ContinueToReviewButton(
                enabled = false,
                onClick = {},
            )
            is WooPosRefundState.Content -> when (state.step) {
                WooPosRefundState.Content.RefundStep.SelectItems -> ContinueToReviewButton(
                    enabled = state.selectedItemIds.isNotEmpty(),
                    onClick = { onEvent(WooPosRefundUIEvent.ContinueToReviewClicked) },
                )
                WooPosRefundState.Content.RefundStep.ReviewRefund -> {
                    WooPosButton(
                        text = stringResource(R.string.continue_button),
                        onClick = {
                            onEvent(WooPosRefundUIEvent.ContinueToConfirmRefundClicked)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!disablePartialRefund) {
                        WooPosOutlinedButton(
                            text = stringResource(R.string.woopos_orders_edit_refund),
                            onClick = {
                                onEvent(WooPosRefundUIEvent.BackToSelectItemsClicked)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                WooPosRefundState.Content.RefundStep.ConfirmRefund -> {
                    WooPosButton(
                        text = stringResource(R.string.woopos_orders_yes_proceed),
                        onClick = { onEvent(WooPosRefundUIEvent.OnRefundConfirmed) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    WooPosOutlinedButton(
                        text = stringResource(R.string.back),
                        onClick = {
                            onEvent(WooPosRefundUIEvent.BackToReviewClicked)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WooPosRefundState.Content.RefundStep.Processing -> {
                    WooPosButton(
                        text = stringResource(R.string.woopos_orders_yes_proceed),
                        onClick = {},
                        state = WooPosButtonState.LOADING,
                        modifier = Modifier.fillMaxWidth()
                    )
                    WooPosOutlinedButton(
                        text = stringResource(R.string.back),
                        onClick = {},
                        state = WooPosButtonState.DISABLED,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WooPosRefundState.Content.RefundStep.PreparingReader,
                is WooPosRefundState.Content.RefundStep.ReadyForRefund -> {
                    WooPosOutlinedButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WooPosRefundState.Content.RefundStep.ReaderDisconnected -> {
                    WooPosButton(
                        text = stringResource(R.string.retry),
                        onClick = { onEvent(WooPosRefundUIEvent.ConnectReaderClicked) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    WooPosOutlinedButton(
                        text = stringResource(R.string.cancel),
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                WooPosRefundState.Content.RefundStep.ProcessingRefund,
                WooPosRefundState.Content.RefundStep.NotifyingStore -> {
                    WooPosButton(
                        text = stringResource(R.string.woopos_orders_yes_proceed),
                        onClick = {},
                        state = WooPosButtonState.LOADING,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            is WooPosRefundState.Error -> {
                WooPosButton(
                    text = stringResource(R.string.retry),
                    onClick = {
                        onEvent(
                            when (state.errorType) {
                                WooPosRefundState.Error.ErrorType.Loading ->
                                    WooPosRefundUIEvent.RetryLoadRefundableItems
                                WooPosRefundState.Error.ErrorType.Processing ->
                                    WooPosRefundUIEvent.RetryCreateRefund
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                WooPosOutlinedButton(
                    text = stringResource(R.string.cancel),
                    onClick = {
                        onEvent(WooPosRefundUIEvent.CancelRefund)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is WooPosRefundState.NoRefundableItems -> {
                WooPosButton(
                    text = stringResource(R.string.close),
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is WooPosRefundState.RefundSuccess -> {
                WooPosButton(
                    text = stringResource(R.string.done),
                    onClick = { onDismiss() },
                    modifier = Modifier.fillMaxWidth()
                )
                WooPosOutlinedButton(
                    text = stringResource(R.string.woopos_receipt_button),
                    onClick = {
                        onNavigationEvent(
                            WooPosNavigationEvent.OpenEmailReceipt(
                                orderId = state.orderId,
                                receiptAlreadySent = state.receiptSentMessage != null,
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ContinueToReviewButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    WooPosButton(
        text = stringResource(R.string.continue_button),
        onClick = onClick,
        state = if (enabled) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ContentStateHandler(
    state: WooPosRefundState.Content,
    orderId: Long,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    onEvent: (WooPosRefundUIEvent) -> Unit,
    modifier: Modifier = Modifier,
    disablePartialRefund: Boolean = false,
) {
    AnimatedContent(
        modifier = modifier,
        targetState = state.step,
        contentKey = { step ->
            when (step) {
                WooPosRefundState.Content.RefundStep.ConfirmRefund,
                WooPosRefundState.Content.RefundStep.Processing -> "confirm"
                WooPosRefundState.Content.RefundStep.PreparingReader -> "reader_preparing"
                WooPosRefundState.Content.RefundStep.ReaderDisconnected -> "reader_disconnected"
                is WooPosRefundState.Content.RefundStep.ReadyForRefund -> "reader_ready"
                WooPosRefundState.Content.RefundStep.ProcessingRefund -> "reader_processing"
                WooPosRefundState.Content.RefundStep.NotifyingStore -> "notifying_store"
                else -> step
            }
        },
        transitionSpec = { refundFadeTransition() },
        label = "refund_step_transition",
    ) { step ->
        when (step) {
            WooPosRefundState.Content.RefundStep.SelectItems -> {
                SelectItemsContent(
                    state = state,
                    onEvent = onEvent,
                    disableItemSelection = disablePartialRefund,
                )
            }

            WooPosRefundState.Content.RefundStep.ReviewRefund -> {
                ReviewRefundContent(
                    state = state,
                    onEditReason = {
                        onNavigationEvent(
                            WooPosNavigationEvent.OpenRefundReason(
                                orderId = orderId,
                                initialReason = state.refundReason
                            )
                        )
                    }
                )
            }

            WooPosRefundState.Content.RefundStep.ConfirmRefund,
            WooPosRefundState.Content.RefundStep.Processing -> {
                ConfirmRefundContent(state = state)
            }

            WooPosRefundState.Content.RefundStep.PreparingReader,
            WooPosRefundState.Content.RefundStep.ReaderDisconnected,
            is WooPosRefundState.Content.RefundStep.ReadyForRefund,
            WooPosRefundState.Content.RefundStep.ProcessingRefund,
            WooPosRefundState.Content.RefundStep.NotifyingStore -> {
                ConfirmRefundContent(state = state)
            }
        }
    }
}

@Composable
private fun ShimmerItemRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.XSmall.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooPosShimmerBox(
            modifier = Modifier
                .size(24.dp.toAdaptiveIconSize())
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value))
        WooPosShimmerBox(
            modifier = Modifier
                .size(56.dp.toAdaptiveIconSize())
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)
        ) {
            WooPosShimmerText(
                text = "Product item name placeholder",
                style = WooPosTypography.BodyLarge.style,
                fontWeight = FontWeight.Bold,
            )
            WooPosShimmerText(
                text = "$18.00",
                style = WooPosTypography.BodyMedium.style,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    errorState: WooPosRefundState.Error,
    modifier: Modifier = Modifier,
) {
    val title = when (errorState.errorType) {
        WooPosRefundState.Error.ErrorType.Loading ->
            stringResource(R.string.woopos_refund_loading_error_title)
        WooPosRefundState.Error.ErrorType.Processing ->
            stringResource(R.string.woopos_refund_creating_error_title)
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(WooPosComponentSize.Small.value),
            imageVector = com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons.ErrorX,
            contentDescription = stringResource(id = R.string.woopos_error_icon_content_description),
            tint = WooPosTheme.colors.unspecified,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

        WooPosText(
            text = title,
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosText(
            text = stringResource(R.string.woopos_refund_error_subtitle),
            style = WooPosTypography.BodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun NoItemsContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_orders_no_items_available_for_refund),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RefundSuccessContent(
    state: WooPosRefundState.RefundSuccess,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosSuccessCheckmark(
            contentDescription = stringResource(R.string.woopos_orders_refund_complete),
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XXXLarge.value))

        WooPosText(
            text = stringResource(R.string.woopos_orders_refund_complete),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosText(
            text = stringResource(
                R.string.woopos_orders_refund_success_message,
                state.refundedAmount,
                state.paymentMethod
            ),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (state.receiptSentMessage != null) {
            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

            WooPosText(
                text = state.receiptSentMessage,
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SelectItemsContent(
    state: WooPosRefundState,
    onEvent: (WooPosRefundUIEvent) -> Unit,
    modifier: Modifier = Modifier,
    disableItemSelection: Boolean = false,
) {
    val contentState = state as? WooPosRefundState.Content
    val isLoading = state is WooPosRefundState.Loading

    Column(modifier = modifier.fillMaxSize()) {
        if (!isLoading) {
            ItemsHeaderRow(
                allItemsSelected = contentState?.allItemsSelected ?: false,
                selectedCount = contentState?.selectedItemIds?.size ?: 0,
                onSelectAllToggled = { onEvent(WooPosRefundUIEvent.SelectAllToggled) },
                enabled = !disableItemSelection,
            )
        }

        Divider()

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = WooPosSpacing.Medium.value),
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
            ) {
                repeat(4) { index ->
                    ShimmerItemRow()
                    if (index < 3) {
                        Divider()
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(vertical = WooPosSpacing.Medium.value),
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
            ) {
                itemsIndexed(contentState?.refundableItems ?: emptyList()) { index, item ->
                    RefundableItemRow(
                        item = item,
                        isSelected = item.uniqueId in (contentState?.selectedItemIds ?: emptySet()),
                        onItemClick = { onEvent(WooPosRefundUIEvent.ItemSelectionToggled(item.uniqueId)) },
                        enabled = !disableItemSelection,
                    )
                    if (index < (contentState?.refundableItems?.lastIndex ?: 0)) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemsHeaderRow(
    allItemsSelected: Boolean,
    selectedCount: Int,
    onSelectAllToggled: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val selectAllContentDescription = stringResource(R.string.order_refunds_items_select_all)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.Medium.value)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSelectAllToggled
                    )
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = allItemsSelected,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier
                .size(WooPosIconSize.Medium.value)
                .semantics {
                    contentDescription = selectAllContentDescription
                },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Large.value))
        val selectAllLabel = @Composable {
            WooPosText(
                text = stringResource(R.string.woopos_orders_select_all_items),
                style = WooPosTypography.Caption,
                fontWeight = FontWeight.Bold,
                color = WooPosTheme.colors.onSurfaceVariantHighest
            )
        }
        val selectedCountLabel = @Composable {
            WooPosText(
                text = stringResource(R.string.woopos_orders_items_selected_count, selectedCount),
                style = WooPosTypography.Caption,
                fontWeight = FontWeight.Normal,
                color = WooPosTheme.colors.onSurfaceVariantLowest
            )
        }
        if (currentWooPosBreakpoint() == WooPosBreakpoint.Phone) {
            Column {
                selectAllLabel()
                selectedCountLabel()
            }
        } else {
            Row {
                selectAllLabel()
                Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
                selectedCountLabel()
            }
        }
    }
}

@Composable
private fun RefundableItemRow(
    item: WooPosRefundableItem,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.XSmall.value)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onItemClick
                    )
                } else {
                    Modifier
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onItemClick() },
            enabled = enabled,
            modifier = Modifier
                .size(WooPosIconSize.Medium.value)
                .semantics {
                    contentDescription = item.name
                },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value))

        if (item.isLumpSum) {
            WooPosCustomAmountTileAvatar(name = item.name)
        } else {
            WooPosItemImage(
                modifier = Modifier
                    .size(WooPosComponentSize.XSmall.value)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value)),
                imageUrl = null,
                placeholderIcon = ImageVector.vectorResource(R.drawable.ic_inventory_2_24dp),
                placeholderIconSize = WooPosIconSize.Small.value
            )
        }
        Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            WooPosText(
                text = item.name,
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            WooPosText(
                text = item.formattedUnitPrice,
                style = WooPosTypography.BodyMedium,
                color = WooPosTheme.colors.onSurfaceVariantHighest
            )
        }
    }
}

@Composable
private fun ReviewRefundContent(
    state: WooPosRefundState.Content,
    onEditReason: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
        ) {
            ReviewSummaryRow(
                label = pluralStringResource(
                    R.plurals.woopos_orders_items_subtotal_count_plural,
                    state.itemsCount,
                    state.itemsCount
                ),
                value = state.formattedSubtotal,
                isTotal = false
            )
            ReviewSummaryRow(
                label = stringResource(R.string.taxes),
                value = state.formattedTaxes,
                isTotal = false
            )

            Divider()

            Column(
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)
            ) {
                ReviewSummaryRow(
                    label = stringResource(R.string.woopos_orders_refund_total),
                    value = state.formattedTotal,
                    isTotal = true
                )
                WooPosText(
                    text = stringResource(R.string.woopos_orders_via_payment_method, state.paymentMethod),
                    style = WooPosTypography.BodyMedium,
                    fontWeight = FontWeight.Normal,
                    color = WooPosTheme.colors.onSurfaceVariantHighest
                )
            }

            Divider()

            val editReasonText = stringResource(R.string.woopos_orders_edit_reason)
            Column(
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                    .clickable(onClick = onEditReason)
                    .semantics {
                        contentDescription = editReasonText
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WooPosText(
                        text = stringResource(R.string.woopos_orders_refund_reason),
                        style = WooPosTypography.BodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    WooPosText(
                        text = editReasonText,
                        style = WooPosTypography.BodyMedium,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.refundReason.isNotBlank()) {
                    WooPosText(
                        text = state.refundReason,
                        style = WooPosTypography.BodyMedium,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = WooPosTheme.colors.onSurfaceVariantHighest
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewSummaryRow(
    label: String,
    value: String,
    isTotal: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WooPosText(
            text = label,
            style = if (isTotal) WooPosTypography.BodyLarge else WooPosTypography.BodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
        WooPosText(
            text = value,
            style = if (isTotal) WooPosTypography.BodyLarge else WooPosTypography.BodyMedium,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun Divider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = WooPosTheme.colors.outlineVariant,
        thickness = 0.25.dp
    )
}

@Composable
private fun ConfirmRefundContent(
    state: WooPosRefundState.Content,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        WooPosText(
            text = stringResource(
                R.string.woopos_orders_confirm_refund_message,
                state.formattedTotal,
                state.paymentMethod
            ),
            style = WooPosTypography.BodyLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

private fun WooPosRefundState.Content.RefundStep.isNonCancelable(): Boolean {
    return when (this) {
        WooPosRefundState.Content.RefundStep.Processing,
        WooPosRefundState.Content.RefundStep.ProcessingRefund,
        WooPosRefundState.Content.RefundStep.NotifyingStore -> true
        WooPosRefundState.Content.RefundStep.SelectItems,
        WooPosRefundState.Content.RefundStep.ReviewRefund,
        WooPosRefundState.Content.RefundStep.ConfirmRefund,
        WooPosRefundState.Content.RefundStep.PreparingReader,
        WooPosRefundState.Content.RefundStep.ReaderDisconnected,
        is WooPosRefundState.Content.RefundStep.ReadyForRefund -> false
    }
}

@WooPosPreview
@Composable
fun SelectItemsContentPreview() {
    val sampleItems = listOf(
        WooPosRefundableItem(
            orderItemId = 1,
            productId = 100,
            variationId = 0,
            name = "Cup",
            unitPrice = BigDecimal("18.00"),
            unitTax = BigDecimal("1.80"),
            formattedUnitPrice = "$18.00",
            formattedUnitTax = "$1.80",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 2,
            productId = 200,
            variationId = 0,
            name = "Coffee Storage Container",
            unitPrice = BigDecimal("30.00"),
            unitTax = BigDecimal("3.00"),
            formattedUnitPrice = "$30.00",
            formattedUnitTax = "$3.00",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 3,
            productId = 300,
            variationId = 0,
            name = "Enamel Mug",
            unitPrice = BigDecimal("8.50"),
            unitTax = BigDecimal("0.85"),
            formattedUnitPrice = "$8.50",
            formattedUnitTax = "$0.85",
            rowIndex = 0
        )
    )

    val state = WooPosRefundState.Content(
        orderId = 123,
        orderNumber = "#123",
        currency = "USD",
        refundableItems = sampleItems,
        selectedItemIds = sampleItems.map { it.uniqueId }.toSet(),
        allItemsSelected = true,
        itemsCount = 3,
        subtotal = BigDecimal("57.00"),
        taxes = BigDecimal("5.65"),
        total = BigDecimal("62.65"),
        formattedSubtotal = "$57.00",
        formattedTaxes = "$5.65",
        formattedTotal = "$62.65",
        paymentMethod = "payment card ••••1456",
        step = WooPosRefundState.Content.RefundStep.SelectItems
    )

    WooPosTheme {
        SelectItemsContent(
            state = state,
            onEvent = {},
        )
    }
}

@WooPosPreview
@Composable
fun ReviewRefundContentPreview() {
    val sampleItems = listOf(
        WooPosRefundableItem(
            orderItemId = 1,
            productId = 100,
            variationId = 0,
            name = "Cup",
            unitPrice = BigDecimal("18.00"),
            unitTax = BigDecimal("1.80"),
            formattedUnitPrice = "$18.00",
            formattedUnitTax = "$1.80",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 2,
            productId = 200,
            variationId = 0,
            name = "Coffee Storage Container",
            unitPrice = BigDecimal("30.00"),
            unitTax = BigDecimal("3.00"),
            formattedUnitPrice = "$30.00",
            formattedUnitTax = "$3.00",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 3,
            productId = 300,
            variationId = 0,
            name = "Enamel Mug",
            unitPrice = BigDecimal("8.50"),
            unitTax = BigDecimal("0.85"),
            formattedUnitPrice = "$8.50",
            formattedUnitTax = "$0.85",
            rowIndex = 0
        )
    )

    val state = WooPosRefundState.Content(
        orderId = 123,
        orderNumber = "#123",
        currency = "USD",
        refundableItems = sampleItems,
        selectedItemIds = sampleItems.map { it.uniqueId }.toSet(),
        allItemsSelected = true,
        itemsCount = 3,
        subtotal = BigDecimal("57.00"),
        taxes = BigDecimal("5.65"),
        total = BigDecimal("62.65"),
        formattedSubtotal = "$57.00",
        formattedTaxes = "$5.65",
        formattedTotal = "$62.65",
        paymentMethod = "payment card ••••1456",
        step = WooPosRefundState.Content.RefundStep.ReviewRefund
    )

    WooPosTheme {
        ReviewRefundContent(
            state = state,
            onEditReason = {}
        )
    }
}

@WooPosPreview
@Composable
fun ConfirmRefundContentPreview() {
    val sampleItems = listOf(
        WooPosRefundableItem(
            orderItemId = 1,
            productId = 100,
            variationId = 0,
            name = "Cup",
            unitPrice = BigDecimal("18.00"),
            unitTax = BigDecimal("1.80"),
            formattedUnitPrice = "$18.00",
            formattedUnitTax = "$1.80",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 2,
            productId = 200,
            variationId = 0,
            name = "Coffee Storage Container",
            unitPrice = BigDecimal("30.00"),
            unitTax = BigDecimal("3.00"),
            formattedUnitPrice = "$30.00",
            formattedUnitTax = "$3.00",
            rowIndex = 0
        ),
        WooPosRefundableItem(
            orderItemId = 3,
            productId = 300,
            variationId = 0,
            name = "Enamel Mug",
            unitPrice = BigDecimal("8.50"),
            unitTax = BigDecimal("0.85"),
            formattedUnitPrice = "$8.50",
            formattedUnitTax = "$0.85",
            rowIndex = 0
        )
    )

    val state = WooPosRefundState.Content(
        orderId = 123,
        orderNumber = "#123",
        currency = "USD",
        refundableItems = sampleItems,
        selectedItemIds = sampleItems.map { it.uniqueId }.toSet(),
        allItemsSelected = true,
        itemsCount = 3,
        subtotal = BigDecimal("57.00"),
        taxes = BigDecimal("5.65"),
        total = BigDecimal("62.65"),
        formattedSubtotal = "$57.00",
        formattedTaxes = "$5.65",
        formattedTotal = "$62.65",
        paymentMethod = "payment card ••••1456",
        step = WooPosRefundState.Content.RefundStep.ConfirmRefund
    )

    WooPosTheme {
        ConfirmRefundContent(
            state = state,
        )
    }
}

@WooPosPreview
@Composable
fun RefundSuccessContentPreview() {
    val state = WooPosRefundState.RefundSuccess(
        orderId = 123,
        orderNumber = "#123",
        refundedAmount = "$62.65",
        paymentMethod = "payment card ••••1456"
    )

    WooPosTheme {
        RefundSuccessContent(
            state = state,
        )
    }
}
