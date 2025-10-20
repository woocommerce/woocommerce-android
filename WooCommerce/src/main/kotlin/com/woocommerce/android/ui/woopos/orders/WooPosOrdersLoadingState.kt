package com.woocommerce.android.ui.woopos.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosOrdersLoadingState() {
    Row(modifier = Modifier.fillMaxSize()) {
        OrdersListLoadingPane(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = WOO_POS_ORDERS_TOOLBAR_HEIGHT)
                .padding(top = WooPosSpacing.XLarge.value)
        )

        OrderDetailsLoadingPane(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(top = WooPosSpacing.Large.value)
        )
    }
}

@Composable
fun WooPosOrdersOrderLoadingRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(
                horizontal = WooPosSpacing.Medium.value,
                vertical = WooPosSpacing.Medium.value
            )
            .heightIn(min = 64.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)) {
            WooPosShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.2f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )
            WooPosShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.35f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )
            WooPosShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.25f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )
            Spacer(Modifier.height(WooPosSpacing.XSmall.value))
            WooPosShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.2f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )
        }

        Spacer(Modifier.weight(1f))

        WooPosShimmerBox(
            modifier = Modifier
                .width(48.dp)
                .height(18.dp)
                .alignByBaseline()
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )
    }
}

@Composable
private fun OrdersListLoadingPane(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        contentPadding = PaddingValues(WooPosSpacing.Medium.value)
    ) {
        items(7) {
            WooPosCard(shadowType = ShadowType.Soft) {
                WooPosOrdersOrderLoadingRow()
            }
        }
    }
}

@Composable
private fun OrderDetailsLoadingPane(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Medium.value)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                WooPosShimmerBox(
                    modifier = Modifier
                        .width(96.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

                WooPosShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

                WooPosShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                )
            }

            WooPosShimmerBox(
                modifier = Modifier
                    .width(140.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            WooPosCard(shadowType = ShadowType.Soft) {
                Column(
                    modifier = Modifier.padding(WooPosSpacing.Medium.value)
                ) {
                    WooPosText(
                        text = stringResource(R.string.woopos_orders_details_products_title),
                        style = WooPosTypography.BodyXLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

                    repeat(3) {
                        ProductLoadingItem()
                        if (it < 2) {
                            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosCard(
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
                elevation = WooPosElevation.Medium,
                shadowType = ShadowType.Soft,
            ) {
                Column(
                    modifier = Modifier.padding(WooPosSpacing.Medium.value)
                ) {
                    WooPosText(
                        text = stringResource(R.string.woopos_orders_details_totals_title),
                        style = WooPosTypography.BodyXLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

                    repeat(3) {
                        TotalLoadingItem()
                        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                    }
                    repeat(2) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                        TotalLoadingItem()
                        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
                    }
                }
            }
        }
    }
}

@Composable
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun ProductLoadingItem() {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.Small.value)
    ) {
        val (image, nameShimmer, qtyShimmer, totalShimmer) = createRefs()

        WooPosShimmerBox(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                .constrainAs(image) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
        )

        WooPosShimmerBox(
            modifier = Modifier
                .width(120.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                .constrainAs(nameShimmer) {
                    top.linkTo(image.top)
                    start.linkTo(image.end, margin = WooPosSpacing.Medium.value)
                }
        )

        WooPosShimmerBox(
            modifier = Modifier
                .width(80.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                .constrainAs(qtyShimmer) {
                    bottom.linkTo(image.bottom)
                    start.linkTo(nameShimmer.start)
                }
        )

        WooPosShimmerBox(
            modifier = Modifier
                .width(60.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                .constrainAs(totalShimmer) {
                    top.linkTo(nameShimmer.top)
                    end.linkTo(parent.end)
                }
        )
    }
}

@Composable
private fun TotalLoadingItem() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        WooPosShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .height(16.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )

        WooPosShimmerBox(
            modifier = Modifier
                .width(60.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersLoadingStatePreview() {
    WooPosTheme {
        WooPosOrdersLoadingState()
    }
}
