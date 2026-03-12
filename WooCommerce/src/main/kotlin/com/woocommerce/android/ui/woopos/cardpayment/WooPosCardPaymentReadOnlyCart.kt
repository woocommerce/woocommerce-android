package com.woocommerce.android.ui.woopos.cardpayment

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosItemImage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosCardPaymentReadOnlyCart(
    cartItems: List<ReadOnlyCartItem>,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceBright)
    ) {
        ReadOnlyCartToolbar(
            itemCount = cartItems.size,
            onBackClicked = onBackClicked,
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        WooPosLazyColumn(
            modifier = Modifier
                .padding(horizontal = WooPosSpacing.Medium.value)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                top = WooPosSpacing.XSmall.value,
                bottom = WooPosSpacing.Small.value
            ),
            withBottomShadow = true,
        ) {
            items(
                cartItems,
                key = { item -> item.id }
            ) { item ->
                ReadOnlyCartProductItem(item = item)
            }
        }
    }
}

@Composable
private fun ReadOnlyCartToolbar(
    itemCount: Int,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooPosBackButton(
            modifier = Modifier.padding(start = WooPosSpacing.Small.value),
            contentDescription = stringResource(R.string.woopos_cart_back_content_description),
            iconModifier = Modifier.size(28.dp),
            onClick = onBackClicked,
        )
        WooPosText(
            text = stringResource(R.string.woopos_cart_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(
                start = WooPosSpacing.Medium.value,
                end = WooPosSpacing.XSmall.value,
            ),
        )
        Spacer(modifier = Modifier.weight(1f))
        WooPosText(
            text = if (itemCount == 1) {
                stringResource(R.string.woopos_items_in_cart, itemCount)
            } else {
                stringResource(R.string.woopos_items_in_cart_multiple, itemCount)
            },
            style = WooPosTypography.BodySmall,
            color = WooPosTheme.colors.onSurfaceVariantLowest,
            maxLines = 1,
            modifier = Modifier.padding(end = WooPosSpacing.Medium.value),
        )
    }
}

@Composable
private fun ReadOnlyCartProductItem(
    item: ReadOnlyCartItem,
    modifier: Modifier = Modifier,
) {
    WooPosCard(
        modifier = modifier.wrapContentHeight(),
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
                    .width(96.dp)
                    .fillMaxHeight()
                    .heightIn(min = 96.dp),
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
                )
                item.subtitle?.let { subtitle ->
                    Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                    WooPosText(
                        text = subtitle,
                        maxLines = 1,
                        style = WooPosTypography.BodySmall,
                        color = WooPosTheme.colors.onSurfaceVariantHighest,
                    )
                }
                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = item.formattedPrice,
                    style = WooPosTypography.BodySmall,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosCardPaymentReadOnlyCartPreview() {
    WooPosTheme {
        WooPosCardPaymentReadOnlyCart(
            cartItems = listOf(
                ReadOnlyCartItem(
                    id = 1L,
                    name = "Booking - Spa Treatment",
                    formattedPrice = "$50.00",
                    imageUrl = null,
                    subtitle = "11:00 AM-12:00 PM",
                ),
                ReadOnlyCartItem(
                    id = 2L,
                    name = "Booking - Hair Styling Session",
                    formattedPrice = "$35.00",
                    imageUrl = null,
                    subtitle = "2:00 PM-3:30 PM",
                ),
            ),
            onBackClicked = {},
        )
    }
}
