package com.woocommerce.android.ui.woopos.home.cart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCustomAmountInitialsAvatar
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosIconButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosItemImage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOverflowMenu
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOverflowMenuItem
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveIconSize
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState.Coupon.CouponValidationState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartUIEvent.ItemRemovedFromCart
import com.woocommerce.android.ui.woopos.util.WooPosTestTags
import java.math.BigDecimal

@Composable
fun WooPosCartScreen(
    checkoutSlot: WooPosCartCheckoutButtonSlot,
    modifier: Modifier = Modifier,
    viewModel: WooPosCartViewModel = hiltViewModel(),
) {
    viewModel.state.observeAsState().value?.let { state ->
        WooPosCartScreen(modifier, state, viewModel::onUIEvent, checkoutSlot = checkoutSlot)
    }
}

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
fun WooPosCartScreen(
    modifier: Modifier = Modifier,
    state: WooPosCartState,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
    onPhoneBack: (() -> Unit)? = null,
    checkoutSlot: WooPosCartCheckoutButtonSlot,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceBright)
    ) {
        val (toolbar, body, checkoutButton) = createRefs()

        CartToolbar(
            modifier = Modifier.constrainAs(toolbar) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            toolbar = state.toolbar,
            onClearAllClicked = { onUIEvent(WooPosCartUIEvent.ClearAllClicked) },
            onBackClicked = { onUIEvent(WooPosCartUIEvent.BackClicked) },
            onPhoneBack = onPhoneBack,
        )

        when (state.body) {
            WooPosCartState.Body.Empty -> {
                CartBodyEmpty(
                    modifier = Modifier.constrainAs(body) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        height = Dimension.fillToConstraints
                    },
                    onBarcodeSetupClicked = { onUIEvent(WooPosCartUIEvent.BarcodeSetupClicked) }
                )
            }

            is WooPosCartState.Body.WithItems -> {
                val productsTopMargin = WooPosSpacing.Large.value
                CartBodyWithItems(
                    modifier = Modifier.constrainAs(body) {
                        top.linkTo(toolbar.bottom, margin = productsTopMargin)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        height = Dimension.fillToConstraints
                    },
                    items = state.body.itemsInCart,
                    areItemsRemovable = state.areItemsRemovable,
                    isCustomAmountDiscountNotAppliedNoteVisible = state.isCustomAmountDiscountNotAppliedNoteVisible,
                    onUIEvent = onUIEvent
                )
            }
        }

        when (checkoutSlot) {
            WooPosCartCheckoutButtonSlot.Inline -> {
                AnimatedVisibility(
                    visible = state.checkoutButtonState != WooPosCartState.CheckoutButtonState.Invisible,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .constrainAs(checkoutButton) {
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        WooPosButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = WooPosSpacing.Medium.value)
                                .navigationBarsPadding()
                                .testTag(WooPosTestTags.CHECKOUT_BUTTON),
                            text = stringResource(R.string.woopos_checkout_button),
                            onClick = { onUIEvent(WooPosCartUIEvent.CheckoutClicked) },
                            state = when (state.checkoutButtonState) {
                                WooPosCartState.CheckoutButtonState.Enabled -> WooPosButtonState.ENABLED
                                WooPosCartState.CheckoutButtonState.Disabled -> WooPosButtonState.DISABLED
                                WooPosCartState.CheckoutButtonState.Invisible -> WooPosButtonState.ENABLED
                            }
                        )
                    }
                }
            }
            WooPosCartCheckoutButtonSlot.External -> Unit
        }
    }
}

@Composable
fun CartBodyEmpty(
    modifier: Modifier = Modifier,
    onBarcodeSetupClicked: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .padding(WooPosSpacing.XLarge.value),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_add_shopping_cart_24dp),
            contentDescription = stringResource(R.string.woopos_cart_empty_content_description),
            modifier = Modifier.size(WooPosComponentSize.Small.value),
            tint = WooPosTheme.colors.onSurfaceVariantLowest.copy(alpha = 0.5F)
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        val annotatedText = buildAnnotatedString {
            append(stringResource(R.string.woopos_cart_empty_subtitle_with_scanner))
            pushStringAnnotation(
                tag = "clickable",
                annotation = "scan_barcode"
            )
            append(" ")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Normal
                )
            ) {
                append(stringResource(R.string.woopos_cart_empty_scan_barcode))
            }
            append(" ")
            pop()
            append(stringResource(R.string.woopos_cart_empty_subtitle_suffix))
        }

        WooPosText(
            text = annotatedText,
            style = WooPosTypography.BodyMedium,
            color = WooPosTheme.colors.onSurfaceVariantLowest,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable { onBarcodeSetupClicked() }
                .padding(
                    horizontal = WooPosSpacing.XLarge.value,
                    vertical = WooPosSpacing.Medium.value,
                )
        )
    }
}

@Composable
private fun CartBodyWithItems(
    modifier: Modifier = Modifier,
    items: List<WooPosCartItemViewState>,
    areItemsRemovable: Boolean,
    isCustomAmountDiscountNotAppliedNoteVisible: Boolean,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    ScrollToTopHandler(items, listState)

    WooPosLazyColumn(
        modifier = modifier
            .padding(horizontal = WooPosSpacing.Medium.value)
            .fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(
            top = WooPosSpacing.XSmall.value,
            bottom = WooPosSpacing.Small.value
        ),
        withBottomShadow = true,
    ) {
        items(
            items,
            key = { item -> item.itemNumber }
        ) { item ->
            when (item) {
                is WooPosCartItemViewState.Product -> ProductItem(
                    modifier = Modifier.animateItem(),
                    item = item,
                    canRemoveItems = areItemsRemovable,
                    onUIEvent = onUIEvent,
                )

                is WooPosCartItemViewState.Coupon -> CouponItem(
                    modifier = Modifier.animateItem(),
                    item = item,
                    canRemoveItems = areItemsRemovable,
                    onUIEvent = onUIEvent,
                )

                is WooPosCartItemViewState.CustomAmount -> CustomAmountItem(
                    modifier = Modifier.animateItem(),
                    item = item,
                    canRemoveItems = areItemsRemovable,
                    isDiscountNotAppliedNoteVisible = isCustomAmountDiscountNotAppliedNoteVisible,
                    onUIEvent = onUIEvent,
                )

                is WooPosCartItemViewState.Error -> ErrorItem(
                    modifier = Modifier.animateItem(),
                    item = item,
                    canRemoveItems = areItemsRemovable,
                    onUIEvent = onUIEvent,
                )

                is WooPosCartItemViewState.Loading -> LoadingItem(
                    modifier = Modifier.animateItem(),
                    item = item,
                    canRemoveItems = areItemsRemovable,
                    onUIEvent = onUIEvent,
                )
            }
        }
    }
}

@Composable
private fun ScrollToTopHandler(
    items: List<WooPosCartItemViewState>,
    listState: LazyListState
) {
    val previousItemsCount = remember { mutableIntStateOf(0) }
    val itemsInCartSize = items.size
    LaunchedEffect(itemsInCartSize) {
        if (itemsInCartSize > previousItemsCount.intValue) {
            listState.animateScrollToItem(0)
        }
        previousItemsCount.intValue = itemsInCartSize
    }
}

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun CartToolbar(
    modifier: Modifier = Modifier,
    toolbar: WooPosCartState.Toolbar,
    onClearAllClicked: () -> Unit,
    onBackClicked: () -> Unit,
    onPhoneBack: (() -> Unit)? = null,
) {
    val iconSize = 28.dp
    val iconTitlePadding = WooPosSpacing.Medium.value
    val titleOffset by animateDpAsState(
        targetValue = if (toolbar.backIconVisible) iconSize + iconTitlePadding else 0.dp,
        animationSpec = tween(durationMillis = 300),
        label = "titleOffset"
    )

    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(WooPosComponentSize.XSmall.value)
    ) {
        val (backButton, title, spacer, itemsCount, clearAllButton) = createRefs()

        when (onPhoneBack) {
            null -> AnimatedVisibility(
                visible = toolbar.backIconVisible,
                enter = fadeIn(animationSpec = tween(300)) + expandHorizontally(),
                exit = fadeOut(animationSpec = tween(300)) + shrinkHorizontally()
            ) {
                WooPosBackButton(
                    modifier = Modifier
                        .constrainAs(backButton) {
                            start.linkTo(parent.start)
                            centerVerticallyTo(parent)
                        }
                        .padding(start = WooPosSpacing.Small.value),
                    contentDescription = stringResource(R.string.woopos_cart_back_content_description),
                    iconModifier = Modifier
                        .size(iconSize)
                        .offset(y = 4.dp)
                ) { onBackClicked() }
            }
            else -> WooPosBackButton(
                modifier = Modifier
                    .constrainAs(backButton) {
                        start.linkTo(parent.start)
                        centerVerticallyTo(parent)
                    }
                    .padding(start = WooPosSpacing.Small.value),
                contentDescription = stringResource(R.string.woopos_cart_back_content_description),
                onClick = onPhoneBack,
            )
        }

        val titleStartMargin = WooPosSpacing.Small.value
        WooPosText(
            text = stringResource(R.string.woopos_cart_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier
                .constrainAs(title) {
                    when (onPhoneBack) {
                        null -> start.linkTo(parent.start, margin = titleOffset)
                        else -> start.linkTo(backButton.end, margin = titleStartMargin)
                    }
                    centerVerticallyTo(parent)
                }
                .padding(
                    start = when (onPhoneBack) {
                        null -> WooPosSpacing.Medium.value
                        else -> 0.dp
                    },
                    end = WooPosSpacing.XSmall.value,
                )
        )

        Spacer(
            modifier = Modifier
                .constrainAs(spacer) {
                    start.linkTo(title.end)
                    end.linkTo(itemsCount.start)
                    width = Dimension.fillToConstraints
                }
        )

        toolbar.itemsCount?.let {
            val itemsEndMargin = WooPosSpacing.Medium.value
            WooPosText(
                text = it,
                style = WooPosTypography.BodySmall,
                color = WooPosTheme.colors.onSurfaceVariantLowest,
                maxLines = 1,
                modifier = Modifier
                    .testTag(WooPosTestTags.CART_ITEMS_COUNT)
                    .constrainAs(itemsCount) {
                        end.linkTo(
                            if (toolbar.isClearAllButtonVisible) {
                                clearAllButton.start
                            } else {
                                parent.end
                            },
                            margin = itemsEndMargin,
                        )
                        centerVerticallyTo(parent)
                    }
            )
        }

        if (toolbar.isClearAllButtonVisible) {
            ClearCartButton(
                modifier = Modifier
                    .constrainAs(clearAllButton) {
                        end.linkTo(parent.end)
                        centerVerticallyTo(parent)
                    }
                    .padding(end = WooPosSpacing.Medium.value),
                onClearCartClicked = onClearAllClicked
            )
        }
    }
}

@Composable
private fun ClearCartButton(
    modifier: Modifier = Modifier,
    onClearCartClicked: () -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val clearCartButtonText = stringResource(R.string.woopos_clear_cart_button)

    Box(modifier = modifier) {
        WooPosIconButton(
            icon = ImageVector.vectorResource(R.drawable.ic_delete_24dp),
            enabled = !dropdownExpanded,
            onClick = { dropdownExpanded = true },
            contentDescription = stringResource(
                id = R.string.woopos_cart_clear_all_button_content_description
            ),
        )

        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(WooPosCornerRadius.Medium.value)
            )
        ) {
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier
                    .defaultMinSize(minWidth = WooPosComponentSize.XLarge.value)
                    .background(color = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                DropdownMenuItem(
                    text = {
                        WooPosText(
                            text = clearCartButtonText,
                            style = WooPosTypography.BodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        dropdownExpanded = false
                        onClearCartClicked()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = clearCartButtonText
                    }
                )
            }
        }
    }
}

@Composable
private fun ProductItem(
    modifier: Modifier = Modifier,
    item: WooPosCartItemViewState.Product,
    canRemoveItems: Boolean,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_cart_item_product_content_description,
        item.name,
        item.price
    )

    WooPosCard(
        modifier = modifier
            .wrapContentHeight()
            .semantics { contentDescription = itemContentDescription },
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WooPosItemImage(
                imageUrl = item.imageUrl,
                modifier = Modifier
                    .width(WooPosComponentSize.Medium.value)
                    .fillMaxHeight()
                    .heightIn(min = WooPosComponentSize.Medium.value),
                placeholderIcon = ImageVector.vectorResource(R.drawable.ic_inventory_2_24dp),
                placeholderIconSize = 36.dp
            )

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = WooPosSpacing.Medium.value)
                    .padding(vertical = WooPosSpacing.Medium.value)
            ) {
                WooPosText(
                    text = item.name,
                    maxLines = 1,
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clearAndSetSemantics { }
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                if (item.description.isNotNullOrEmpty()) {
                    WooPosText(
                        text = item.description ?: "",
                        style = WooPosTypography.BodySmall,
                        color = WooPosTheme.colors.onSurfaceVariantHighest,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clearAndSetSemantics { }
                    )
                    Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                }
                WooPosText(
                    text = item.price,
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    modifier = Modifier
                        .clearAndSetSemantics { }
                )

                if (item.productDoesNotExist) {
                    Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                    WooPosText(
                        text = stringResource(R.string.woopos_cart_product_unknown_item),
                        style = WooPosTypography.BodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.clearAndSetSemantics { }
                    )
                }
            }

            if (canRemoveItems) {
                RemoveItemFromCartButton(item, onUIEvent)
            }
            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
        }
    }
}

@Composable
private fun CouponItem(
    modifier: Modifier = Modifier,
    item: WooPosCartItemViewState.Coupon,
    canRemoveItems: Boolean,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_cart_item_coupon_content_description,
        item.name
    )

    WooPosCard(
        modifier = modifier
            .wrapContentHeight()
            .semantics { contentDescription = itemContentDescription },
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(
                        when (item.validationState) {
                            CouponValidationState.Invalid -> MaterialTheme.colorScheme.error
                            CouponValidationState.Unknown -> MaterialTheme.colorScheme.surfaceDim
                            is CouponValidationState.Valid -> WooPosTheme.colors.success
                        }
                    )
                    .width(WooPosComponentSize.Medium.value)
                    .fillMaxHeight()
                    .heightIn(min = WooPosComponentSize.Medium.value),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_sell_24dp),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        when (item.validationState) {
                            CouponValidationState.Invalid -> MaterialTheme.colorScheme.onError
                            CouponValidationState.Unknown -> WooPosTheme.colors.onSurfaceVariantLowest
                            is CouponValidationState.Valid -> WooPosTheme.colors.onSuccess
                        }
                    ),
                    modifier = Modifier.size(36.dp.toAdaptiveIconSize())
                )
            }

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = WooPosSpacing.Medium.value)
                    .padding(vertical = WooPosSpacing.Medium.value)
            ) {
                WooPosText(
                    text = item.name,
                    maxLines = 1,
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clearAndSetSemantics { }
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = item.summary,
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics { }
                )
                when (item.validationState) {
                    CouponValidationState.Unknown -> Unit
                    CouponValidationState.Invalid -> {
                        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                        WooPosText(
                            text = stringResource(R.string.woopos_cart_coupon_invalid_subtitle),
                            style = WooPosTypography.BodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                    }

                    is CouponValidationState.Valid -> {
                        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                        WooPosText(
                            text = item.validationState.formattedDiscount,
                            style = WooPosTypography.BodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = WooPosTheme.colors.success,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                    }
                }
            }

            if (canRemoveItems) {
                RemoveItemFromCartButton(item, onUIEvent)
            }
            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
        }
    }
}

@Composable
private fun CustomAmountItem(
    modifier: Modifier = Modifier,
    item: WooPosCartItemViewState.CustomAmount,
    canRemoveItems: Boolean,
    isDiscountNotAppliedNoteVisible: Boolean,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_cart_item_custom_amount_content_description,
        item.name,
        item.formattedAmount,
    )

    WooPosCard(
        modifier = modifier
            .wrapContentHeight()
            .semantics { contentDescription = itemContentDescription },
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooPosCustomAmountInitialsAvatar(
                name = item.name,
                modifier = Modifier
                    .width(WooPosComponentSize.Medium.value)
                    .fillMaxHeight()
                    .heightIn(min = WooPosComponentSize.Medium.value),
            )

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = WooPosSpacing.Medium.value)
                    .padding(vertical = WooPosSpacing.Medium.value),
            ) {
                WooPosText(
                    text = item.name,
                    maxLines = 1,
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = item.formattedAmount,
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics { },
                )
                if (item.isTaxable) {
                    Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                    WooPosText(
                        text = stringResource(R.string.woopos_cart_custom_amount_includes_tax),
                        style = WooPosTypography.BodySmall,
                        color = WooPosTheme.colors.onSurfaceVariantLowest,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
                if (isDiscountNotAppliedNoteVisible) {
                    Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                    WooPosText(
                        text = stringResource(R.string.woopos_cart_custom_amount_discount_not_applied),
                        style = WooPosTypography.BodySmall,
                        color = WooPosTheme.colors.alert,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clearAndSetSemantics { },
                    )
                }
            }

            // Edit lives inside the overflow menu, so this also gates the edit path during checkout
            // — intentional: cart contents are locked once payment starts.
            if (canRemoveItems) {
                CustomAmountOverflowMenu(item = item, onUIEvent = onUIEvent)
            }
            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
        }
    }
}

@Composable
private fun CustomAmountOverflowMenu(
    item: WooPosCartItemViewState.CustomAmount,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    WooPosOverflowMenu(
        items = listOf(
            WooPosOverflowMenuItem(
                label = stringResource(R.string.woopos_cart_custom_amount_menu_edit),
                onClick = { onUIEvent(WooPosCartUIEvent.EditCustomAmountClicked(item)) },
            ),
            WooPosOverflowMenuItem(
                label = stringResource(R.string.woopos_cart_custom_amount_menu_remove),
                color = MaterialTheme.colorScheme.error,
                onClick = { onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(item)) },
            ),
        )
    )
}

@Composable
private fun LoadingItem(
    modifier: Modifier = Modifier,
    item: WooPosCartItemViewState.Loading,
    canRemoveItems: Boolean,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_cart_item_loading_content_description,
        item.name
    )
    WooPosCard(
        modifier = modifier
            .height(WooPosComponentSize.Medium.value)
            .semantics { contentDescription = itemContentDescription },
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(WooPosComponentSize.Medium.value)
            ) {
                WooPosShimmerBox(
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = WooPosSpacing.Medium.value)
            ) {
                WooPosShimmerText(
                    text = item.name,
                    style = WooPosTypography.BodySmall.style,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

                WooPosShimmerText(
                    text = "$10.00",
                    style = WooPosTypography.BodySmall.style
                )
            }

            if (canRemoveItems) {
                RemoveItemFromCartButton(item, onUIEvent)
            }
            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
        }
    }
}

@Composable
private fun ErrorItem(
    modifier: Modifier = Modifier,
    item: WooPosCartItemViewState.Error,
    canRemoveItems: Boolean,
    onUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    val itemContentDescription = stringResource(
        id = R.string.woopos_cart_item_error_content_description,
        item.name
    )

    WooPosCard(
        modifier = modifier
            .wrapContentHeight()
            .semantics { contentDescription = itemContentDescription },
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.error)
                    .width(WooPosComponentSize.Medium.value)
                    .fillMaxHeight()
                    .heightIn(min = WooPosComponentSize.Medium.value),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_inventory_2_24dp),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onError),
                    modifier = Modifier.size(36.dp.toAdaptiveIconSize())
                )
            }

            Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = WooPosSpacing.Medium.value)
                    .padding(vertical = WooPosSpacing.Medium.value)
            ) {
                WooPosText(
                    text = item.name,
                    maxLines = 1,
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clearAndSetSemantics { }
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

                WooPosText(
                    text = item.message,
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }

            if (canRemoveItems) {
                RemoveItemFromCartButton(item, onUIEvent)
            }
            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
        }
    }
}

@Composable
private fun RemoveItemFromCartButton(
    item: WooPosCartItemViewState,
    onUIEvent: (WooPosCartUIEvent) -> Unit
) {
    val removeButtonContentDescription = stringResource(
        id = R.string.woopos_remove_item_button_from_cart_content_description,
        item.name
    )
    IconButton(
        onClick = { onUIEvent(ItemRemovedFromCart(item)) },
        modifier = Modifier
            .size(WooPosIconSize.XLarge.value)
            .semantics { contentDescription = removeButtonContentDescription }
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_delete_24dp),
            tint = WooPosTheme.colors.onSurfaceVariantHighest,
            contentDescription = null,
        )
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenProductsPreview(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosCartScreen(
            modifier = modifier,
            state = WooPosCartState(
                toolbar = WooPosCartState.Toolbar(
                    backIconVisible = false,
                    itemsCount = "3 items",
                    isClearAllButtonVisible = true
                ),
                body = WooPosCartState.Body.WithItems(
                    itemsInCart = listOf(
                        WooPosCartItemViewState.Coupon(
                            itemNumber = 1,
                            name = "Test Coupon",
                            summary = "50% Off · All Products",
                            id = 1L,
                            validationState = CouponValidationState.Valid("-$10")
                        ),
                        WooPosCartItemViewState.Coupon(
                            itemNumber = 2,
                            name = "Test Coupon",
                            summary = "10$ off * All Products",
                            id = 1L,
                            validationState = CouponValidationState.Invalid
                        ),
                        WooPosCartItemViewState.Product.Simple(
                            itemNumber = 3,
                            id = 1L,
                            imageUrl = "",
                            name = "VW California, VW California VW California, VW California VW California, " +
                                "VW California VW California, VW California,VW California",
                            description = "test description",
                            price = "€50,000",
                        ),
                        WooPosCartItemViewState.Product.Simple(
                            itemNumber = 4,
                            id = 2L,
                            imageUrl = "",
                            name = "VW California",
                            description = "test description test description test description test description" +
                                " test description test description test description test description test description",
                            price = "$150,000",
                            productDoesNotExist = true,
                        ),
                        WooPosCartItemViewState.Product.Simple(
                            itemNumber = 5,
                            id = 3L,
                            imageUrl = "",
                            name = "VW California",
                            description = "description bla",
                            price = "€250,000",
                        )
                    )
                ),
                areItemsRemovable = true,
                checkoutButtonState = WooPosCartState.CheckoutButtonState.Enabled
            ),
            onUIEvent = {},
            checkoutSlot = WooPosCartCheckoutButtonSlot.Inline,
        )
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenCheckoutPreview(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosCartScreen(
            modifier = modifier,
            state = WooPosCartState(
                toolbar = WooPosCartState.Toolbar(
                    backIconVisible = true,
                    itemsCount = "3 items",
                    isClearAllButtonVisible = true
                ),
                body = WooPosCartState.Body.WithItems(
                    itemsInCart = listOf(
                        WooPosCartItemViewState.Coupon(
                            itemNumber = 1,
                            name = "Test Coupon",
                            summary = "50% Off · 1 Product 50% Off · 1 Product 50% Off · 1 Product " +
                                "50% Off · 1 Product 50% Off · 1 Product 50% Off · 1 Product 50% Off · 1 Product " +
                                "50% Off · 1 Product 50% Off · 1 Product 50% Off · 1 Product 50% Off · 1 Product " +
                                "50% Off · 1 Product 50% Off · 1 Product 50% Off · 1 Product 50% Off · 1 Product",
                            id = 1L,
                            validationState = CouponValidationState.Valid("-$10")
                        ),
                        WooPosCartItemViewState.Coupon(
                            itemNumber = 2,
                            name = "Test Coupon",
                            summary = "10$ off * All Products",
                            id = 1L,
                            validationState = CouponValidationState.Invalid
                        ),
                        WooPosCartItemViewState.Product.Simple(
                            itemNumber = 3,
                            id = 1L,
                            imageUrl = "",
                            name = "VW California",
                            description = null,
                            price = "€50,000",
                        ),
                        WooPosCartItemViewState.Product.Simple(
                            itemNumber = 4,
                            id = 2L,
                            imageUrl = "",
                            name = "VW California",
                            description = null,
                            price = "$150,000",
                        ),
                        WooPosCartItemViewState.Product.Simple(
                            itemNumber = 5,
                            id = 3L,
                            imageUrl = "",
                            name = "VW California",
                            description = null,
                            price = "€250,000",
                            productDoesNotExist = true,
                        ),
                        WooPosCartItemViewState.CustomAmount(
                            itemNumber = 6,
                            name = "Gift wrapping",
                            amount = BigDecimal("5.00"),
                            formattedAmount = "$5.00",
                            isTaxable = true,
                        )
                    )
                ),
                areItemsRemovable = false,
                checkoutButtonState = WooPosCartState.CheckoutButtonState.Enabled,
                isCustomAmountDiscountNotAppliedNoteVisible = true,
            ),
            onUIEvent = {},
            checkoutSlot = WooPosCartCheckoutButtonSlot.Inline,
        )
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenEmptyPreview(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosCartScreen(
            modifier = modifier,
            state = WooPosCartState(
                toolbar = WooPosCartState.Toolbar(
                    backIconVisible = false,
                    itemsCount = null,
                    isClearAllButtonVisible = false
                ),
                body = WooPosCartState.Body.Empty,
                areItemsRemovable = false,
                checkoutButtonState = WooPosCartState.CheckoutButtonState.Invisible
            ),
            onUIEvent = {},
            checkoutSlot = WooPosCartCheckoutButtonSlot.Inline,
        )
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenErrorLoadingPreview(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosCartScreen(
            modifier = modifier,
            state = WooPosCartState(
                toolbar = WooPosCartState.Toolbar(
                    backIconVisible = false,
                    itemsCount = "3 items",
                    isClearAllButtonVisible = true
                ),
                body = WooPosCartState.Body.WithItems(
                    itemsInCart = listOf(
                        WooPosCartItemViewState.Loading(
                            itemNumber = 1,
                            name = "ADS@#DXASDDZA"
                        ),
                        WooPosCartItemViewState.Loading(
                            itemNumber = 2,
                            name = "nADAXS1313XDVZX"
                        ),
                        WooPosCartItemViewState.Error(
                            itemNumber = 3,
                            name = "ADAXS1313XDVZX",
                            message = "Product does not exist"
                        ),
                    )
                ),
                areItemsRemovable = true,
                checkoutButtonState = WooPosCartState.CheckoutButtonState.Disabled
            ),
            onUIEvent = {},
            checkoutSlot = WooPosCartCheckoutButtonSlot.Inline,
        )
    }
}

enum class WooPosCartCheckoutButtonSlot {
    Inline,
    External,
}
