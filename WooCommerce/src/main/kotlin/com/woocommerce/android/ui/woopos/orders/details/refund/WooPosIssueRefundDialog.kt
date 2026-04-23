package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosItemImage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmark
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSuccessCheckmarkAnimationStage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveIconSize
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import java.math.BigDecimal

@Composable
fun WooPosIssueRefundScreen(
    orderId: Long,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    refundReasonUpdate: String? = null,
) {
    val viewModel: WooPosRefundViewModel =
        hiltViewModel<WooPosRefundViewModel, WooPosRefundViewModel.Factory> { factory ->
            factory.create(orderId)
        }

    LaunchedEffect(Unit) {
        viewModel.onUIEvent(WooPosRefundUIEvent.DialogOpened)
    }

    refundReasonUpdate?.let { reason ->
        LaunchedEffect(reason) {
            viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundReasonChanged(reason))
        }
    }

    val handleDismiss = {
        if (viewModel.onDismissRequest()) {
            viewModel.onUIEvent(WooPosRefundUIEvent.DialogDismissed)
            onNavigationEvent(
                WooPosNavigationEvent.GoBackWithResult(
                    key = ISSUE_REFUND_DISMISSED_KEY,
                    value = true
                )
            )
        }
    }

    val handleEvent: (WooPosRefundUIEvent) -> Unit = { event ->
        viewModel.onUIEvent(event)
    }

    BackHandler {
        handleDismiss()
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(
                    start = WooPosSpacing.Medium.value,
                    end = WooPosSpacing.Medium.value,
                    bottom = WooPosSpacing.XLarge.value,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(WooPosComponentSize.XSmall.value))

            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 250,
                            delayMillis = 200,
                            easing = FastOutSlowInEasing
                        )
                    ) togetherWith
                        fadeOut(
                            animationSpec = tween(
                                durationMillis = 200,
                                easing = FastOutSlowInEasing
                            )
                        )
                },
                label = "refund_state_transition",
            ) { animatedState ->
                when (animatedState) {
                    is WooPosRefundState.Loading -> SelectItemsContent(
                        state = animatedState,
                        onEvent = {},
                        onContinue = {},
                    )
                    is WooPosRefundState.Content -> ContentStateHandler(
                        state = animatedState,
                        orderId = orderId,
                        viewModel = viewModel,
                        onNavigationEvent = onNavigationEvent,
                        onEvent = handleEvent,
                    )
                    is WooPosRefundState.Error -> ErrorContent(
                        animatedState,
                        handleEvent,
                        handleDismiss
                    )
                    is WooPosRefundState.NoRefundableItems -> NoItemsContent(handleDismiss)
                    is WooPosRefundState.RefundSuccess -> RefundSuccessContent(
                        state = animatedState,
                        onDismissRequest = handleDismiss,
                        onNavigationEvent = onNavigationEvent
                    )
                }
            }
        }

        WooPosToolbar(
            titleText = stringResource(R.string.orderdetail_issue_refund_button),
            onBackClicked = { handleDismiss() },
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        )
    }
}

@Composable
private fun ContentStateHandler(
    state: WooPosRefundState.Content,
    orderId: Long,
    viewModel: WooPosRefundViewModel,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    onEvent: (WooPosRefundUIEvent) -> Unit,
) {
    AnimatedContent(
        targetState = state.step,
        transitionSpec = {
            fadeIn(
                animationSpec = tween(
                    durationMillis = 250,
                    delayMillis = 200,
                    easing = FastOutSlowInEasing
                )
            ) togetherWith
                fadeOut(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
        },
        label = "refund_step_transition",
    ) { step ->
        when (step) {
            WooPosRefundState.Content.RefundStep.SelectItems -> {
                SelectItemsContent(
                    state = state,
                    onEvent = onEvent,
                    onContinue = {
                        viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToReviewClicked)
                    },
                )
            }

            WooPosRefundState.Content.RefundStep.ReviewRefund -> {
                ReviewRefundContent(
                    state = state,
                    onContinue = {
                        viewModel.onUIEvent(WooPosRefundUIEvent.ContinueToConfirmRefundClicked)
                    },
                    onEditRefund = {
                        viewModel.onUIEvent(WooPosRefundUIEvent.BackToSelectItemsClicked)
                    },
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

            WooPosRefundState.Content.RefundStep.ConfirmRefund -> {
                ConfirmRefundContent(
                    state = state,
                    isProcessing = false,
                    onConfirm = {
                        viewModel.onUIEvent(WooPosRefundUIEvent.OnRefundConfirmed)
                    },
                    onBack = {
                        viewModel.onUIEvent(WooPosRefundUIEvent.BackToReviewClicked)
                    }
                )
            }
            WooPosRefundState.Content.RefundStep.Processing -> {
                ConfirmRefundContent(
                    state = state,
                    isProcessing = true,
                    onConfirm = {},
                    onBack = {}
                )
            }
        }
    }
}

@Composable
private fun ShimmerItemRow() {
    Row(
        modifier = Modifier
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
    onEvent: (WooPosRefundUIEvent) -> Unit,
    onDismissRequest: () -> Unit
) {
    val (title, retryEvent) = when (errorState.errorType) {
        WooPosRefundState.Error.ErrorType.Loading -> {
            stringResource(R.string.woopos_refund_loading_error_title) to
                WooPosRefundUIEvent.RetryLoadRefundableItems
        }
        WooPosRefundState.Error.ErrorType.Processing -> {
            stringResource(R.string.woopos_refund_creating_error_title) to
                WooPosRefundUIEvent.RetryCreateRefund
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
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

        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

        WooPosButton(
            text = stringResource(R.string.retry),
            onClick = { onEvent(retryEvent) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosOutlinedButton(
            text = stringResource(R.string.cancel),
            onClick = {
                onEvent(WooPosRefundUIEvent.CancelRefund)
                onDismissRequest()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun NoItemsContent(onDismissRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_orders_no_items_available_for_refund),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(WooPosSpacing.Large.value))
        WooPosButton(
            text = stringResource(R.string.close),
            onClick = onDismissRequest
        )
    }
}

@Composable
private fun RefundSuccessContent(
    state: WooPosRefundState.RefundSuccess,
    onDismissRequest: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    val animationStage = remember { mutableStateOf(WooPosSuccessCheckmarkAnimationStage.INITIAL) }
    val hugeSpacing = WooPosSpacing.Huge.value
    val mediumSpacing = WooPosSpacing.Medium.value
    val marginBetweenButtonAndText by animateDpAsState(
        targetValue = if (animationStage.value >= WooPosSuccessCheckmarkAnimationStage.BUTTONS) {
            hugeSpacing
        } else {
            mediumSpacing
        },
        label = "Margin between button and text"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            WooPosSuccessCheckmark(
                contentDescription = stringResource(R.string.woopos_orders_refund_complete),
                onAnimationStageChanged = { stage -> animationStage.value = stage }
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

        Spacer(modifier = Modifier.height(marginBetweenButtonAndText))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
        ) {
            WooPosButton(
                text = stringResource(R.string.done),
                onClick = onDismissRequest,
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

@Composable
private fun SelectItemsContent(
    state: WooPosRefundState,
    onEvent: (WooPosRefundUIEvent) -> Unit,
    onContinue: () -> Unit,
    disableItemSelection: Boolean = false
) {
    val contentState = state as? WooPosRefundState.Content
    val isLoading = state is WooPosRefundState.Loading

    Column(modifier = Modifier.fillMaxWidth()) {
        RefundDialogHeader()

        if (!isLoading) {
            ItemsHeaderRow(
                allItemsSelected = contentState?.allItemsSelected ?: false,
                selectedCount = contentState?.selectedItemIds?.size ?: 0,
                onSelectAllToggled = { onEvent(WooPosRefundUIEvent.SelectAllToggled) },
                enabled = !disableItemSelection
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
                repeat(2) { index ->
                    ShimmerItemRow()
                    if (index < 1) {
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
                        enabled = !disableItemSelection
                    )
                    if (index < (contentState?.refundableItems?.lastIndex ?: 0)) {
                        Divider()
                    }
                }
            }
        }

        Divider()

        WooPosButton(
            text = stringResource(R.string.continue_button),
            onClick = onContinue,
            state = if (!isLoading && contentState?.selectedItemIds?.isNotEmpty() == true) {
                WooPosButtonState.ENABLED
            } else {
                WooPosButtonState.DISABLED
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WooPosSpacing.XLarge.value)
        )
    }
}

@Composable
private fun RefundDialogHeader() {
    WooPosText(
        text = stringResource(R.string.woopos_orders_select_items_to_refund),
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WooPosSpacing.XLarge.value)
    )
}

@Composable
private fun ItemsHeaderRow(
    allItemsSelected: Boolean,
    selectedCount: Int,
    onSelectAllToggled: () -> Unit,
    enabled: Boolean = true
) {
    val selectAllContentDescription = stringResource(R.string.order_refunds_items_select_all)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WooPosSpacing.Medium.value)
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
        Row {
            WooPosText(
                text = stringResource(R.string.woopos_orders_select_all_items),
                style = WooPosTypography.Caption,
                fontWeight = FontWeight.Bold,
                color = WooPosTheme.colors.onSurfaceVariantHighest
            )
            Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
            WooPosText(
                text = stringResource(R.string.woopos_orders_items_selected_count, selectedCount),
                style = WooPosTypography.Caption,
                fontWeight = FontWeight.Normal,
                color = WooPosTheme.colors.onSurfaceVariantLowest
            )
        }
    }
}

@Composable
private fun RefundableItemRow(
    item: WooPosRefundableItem,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
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

        WooPosItemImage(
            modifier = Modifier
                .size(WooPosComponentSize.XSmall.value)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value)),
            imageUrl = null,
            placeholderIcon = ImageVector.vectorResource(R.drawable.ic_inventory_2_24dp),
            placeholderIconSize = WooPosIconSize.Small.value
        )
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
    onContinue: () -> Unit,
    onEditRefund: () -> Unit,
    onEditReason: () -> Unit,
    showEditRefundButton: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ReviewRefundHeader()

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

        ReviewActionButtons(
            onContinue = onContinue,
            onEditRefund = onEditRefund,
            showEditRefundButton = showEditRefundButton
        )
    }
}

@Composable
private fun ReviewRefundHeader() {
    WooPosText(
        text = stringResource(R.string.woopos_orders_review_refund),
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WooPosSpacing.XLarge.value)
    )
}

@Composable
private fun ReviewSummaryRow(
    label: String,
    value: String,
    isTotal: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
private fun ReviewActionButtons(
    onContinue: () -> Unit,
    onEditRefund: () -> Unit,
    showEditRefundButton: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = WooPosSpacing.XLarge.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        WooPosButton(
            text = stringResource(R.string.continue_button),
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
        if (showEditRefundButton) {
            WooPosOutlinedButton(
                text = stringResource(R.string.woopos_orders_edit_refund),
                onClick = onEditRefund,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ConfirmRefundHeader(
            title = stringResource(R.string.woopos_orders_confirm_refund_title, state.formattedTotal)
        )

        ConfirmRefundMessage(
            message = stringResource(
                R.string.woopos_orders_confirm_refund_message,
                state.formattedTotal,
                state.paymentMethod
            )
        )

        ConfirmRefundButtons(
            isProcessing = isProcessing,
            onConfirm = onConfirm,
            onBack = onBack
        )
    }
}

@Composable
private fun ConfirmRefundHeader(title: String) {
    WooPosText(
        text = title,
        style = WooPosTypography.Heading,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WooPosSpacing.XLarge.value)
    )
}

@Composable
private fun ConfirmRefundMessage(message: String) {
    WooPosText(
        text = message,
        style = WooPosTypography.BodyLarge,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = WooPosSpacing.XLarge.value)
    )
}

@Composable
private fun ConfirmRefundButtons(
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        WooPosButton(
            text = stringResource(R.string.woopos_orders_yes_proceed),
            onClick = onConfirm,
            state = if (isProcessing) WooPosButtonState.LOADING else WooPosButtonState.ENABLED,
            modifier = Modifier.fillMaxWidth()
        )
        WooPosOutlinedButton(
            text = stringResource(R.string.back),
            onClick = onBack,
            state = if (isProcessing) WooPosButtonState.DISABLED else WooPosButtonState.ENABLED,
            modifier = Modifier.fillMaxWidth()
        )
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
            onContinue = {}
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
            onContinue = {},
            onEditRefund = {},
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
            isProcessing = false,
            onConfirm = {},
            onBack = {}
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
            onDismissRequest = {},
            onNavigationEvent = {}
        )
    }
}
