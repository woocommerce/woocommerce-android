package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosItemImage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel

@Suppress("LongMethod")
@Composable
fun WooPosPhoneCheckoutScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
) {
    val cartViewModel: WooPosCartViewModel = hiltViewModel()
    val cartState by cartViewModel.state.observeAsState()

    val totalsViewModel: WooPosTotalsViewModel = hiltViewModel()
    val totalsState = totalsViewModel.state.collectAsState().value

    var isOrderSummaryExpanded by remember { mutableStateOf(false) }

    val items = (cartState?.body as? WooPosCartState.Body.WithItems)?.itemsInCart ?: emptyList()
    val itemCount = cartState?.body?.amountOfItems ?: 0

    val showOrderSummary = itemCount > 0 && totalsState is WooPosTotalsViewState.Checkout

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    horizontal = WooPosSpacing.Small.value,
                    vertical = WooPosSpacing.Small.value
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_back_24dp),
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
            WooPosText(
                text = "Checkout",
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
            )
        }

        // Totals screen content (reader status + totals grid)
        WooPosTotalsScreen(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

        // Bottom section: order summary + buttons (only when checkout state)
        if (showOrderSummary) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            ) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.dp,
                )

                // Order summary header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isOrderSummaryExpanded = !isOrderSummaryExpanded }
                        .padding(
                            horizontal = WooPosSpacing.Large.value,
                            vertical = WooPosSpacing.Medium.value,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WooPosText(
                        text = "Order summary",
                        style = WooPosTypography.BodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
                    WooPosText(
                        text = "$itemCount items",
                        style = WooPosTypography.BodySmall,
                        color = WooPosTheme.colors.onSurfaceVariantHighest,
                    )
                    Spacer(modifier = Modifier.weight(1f))

                    val totalText = (totalsState as? WooPosTotalsViewState.Checkout)
                        ?.totals
                        ?.let { it as? WooPosTotalsViewState.Totals.Visible }
                        ?.orderTotalText
                    if (totalText != null) {
                        WooPosText(
                            text = totalText,
                            style = WooPosTypography.BodyLarge,
                        )
                        Spacer(modifier = Modifier.width(WooPosSpacing.Small.value))
                    }

                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                        contentDescription = if (isOrderSummaryExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(if (isOrderSummaryExpanded) 90f else -90f),
                    )
                }

                // Expanded product cards (same card design as cart)
                AnimatedVisibility(
                    visible = isOrderSummaryExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(horizontal = WooPosSpacing.Large.value),
                        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
                        contentPadding = PaddingValues(bottom = WooPosSpacing.Medium.value),
                    ) {
                        items(
                            items = items.filterIsInstance<WooPosCartItemViewState.Product>(),
                            key = { it.itemNumber }
                        ) { product ->
                            OrderSummaryProductCard(product = product)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

                // Payment buttons pinned at bottom
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = WooPosSpacing.Large.value)
                        .padding(bottom = WooPosSpacing.Large.value),
                    verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
                ) {
                    val checkoutState = totalsState as? WooPosTotalsViewState.Checkout
                    val readerStatus = checkoutState?.readerStatus
                    if (readerStatus is WooPosTotalsViewState.ReaderStatus.Disconnected) {
                        WooPosButton(
                            text = readerStatus.actionButtonLabel,
                            onClick = {
                                totalsViewModel.onUIEvent(WooPosTotalsUIEvent.ConnectReaderClicked)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )
                    }
                    WooPosOutlinedButton(
                        text = stringResource(R.string.woopos_payment_take_cash_payment_label),
                        onClick = { totalsViewModel.onUIEvent(WooPosTotalsUIEvent.OnCashPaymentClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )
                }
            } // end white background Column
        }
    }
}

@Composable
private fun OrderSummaryProductCard(
    product: WooPosCartItemViewState.Product,
    modifier: Modifier = Modifier,
) {
    WooPosCard(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shape = RoundedCornerShape(WooPosCornerRadius.Small.value),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WooPosItemImage(
                imageUrl = product.imageUrl,
                modifier = Modifier
                    .width(84.dp)
                    .fillMaxHeight()
                    .heightIn(min = 84.dp),
                placeholderIcon = ImageVector.vectorResource(R.drawable.ic_inventory_2_24dp),
                placeholderIconSize = 28.dp,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = WooPosSpacing.Medium.value)
                    .padding(vertical = WooPosSpacing.Small.value)
            ) {
                WooPosText(
                    text = product.name,
                    maxLines = 1,
                    style = WooPosTypography.BodyLarge,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = product.price,
                    style = WooPosTypography.BodyLarge,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
            }
        }
    }
}
