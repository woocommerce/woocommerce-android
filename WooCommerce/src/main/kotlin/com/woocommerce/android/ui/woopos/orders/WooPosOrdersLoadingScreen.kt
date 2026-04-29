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
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveComponentSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveIconSize

@Composable
fun WooPosOrdersLoadingScreen(
    modifier: Modifier = Modifier,
    isPhoneLayout: Boolean = false,
) {
    if (isPhoneLayout) {
        WooPosOrdersListLoadingPane(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(top = WOO_POS_ORDERS_TOOLBAR_HEIGHT + WooPosSpacing.Small.value)
        )
    } else {
        Row(
            modifier = modifier.fillMaxSize()
        ) {
            WooPosOrdersListLoadingPane(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceBright)
                    .padding(top = WOO_POS_ORDERS_TOOLBAR_HEIGHT + WooPosSpacing.Small.value)
                    .weight(0.3f)
                    .fillMaxHeight()
            )

            OrderDetailsLoadingPane(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = WooPosSpacing.Medium.value)
            )
        }
    }
}

@Composable
fun WooPosOrdersOrderLoadingRow() {
    WooPosCard(shadowType = ShadowType.Soft) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(
                    horizontal = WooPosSpacing.Medium.value,
                    vertical = WooPosSpacing.Medium.value
                )
                .heightIn(min = 64.dp.toAdaptiveComponentSize()),
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)) {
                WooPosShimmerText(
                    text = "Order #123",
                    style = WooPosTypography.BodySmall.style,
                    fontWeight = FontWeight.Bold
                )
                WooPosShimmerText(
                    text = "January 1, 2024 at 12:00 PM",
                    style = WooPosTypography.BodySmall.style
                )
                WooPosShimmerText(
                    text = "customer@example.com",
                    style = WooPosTypography.BodySmall.style
                )
                Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                WooPosShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .height(16.dp.toAdaptiveIconSize())
                        .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                )
            }

            Spacer(Modifier.weight(1f))

            WooPosShimmerText(
                text = "$100.00",
                style = WooPosTypography.BodySmall.style
            )
        }
    }
}

@Composable
fun WooPosOrdersListLoadingPane(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        contentPadding = PaddingValues(WooPosSpacing.Medium.value)
    ) {
        items(7) {
            WooPosOrdersOrderLoadingRow()
        }
    }
}

@Composable
fun OrderDetailsLoadingPane(
    modifier: Modifier = Modifier,
    showOrderNumber: Boolean = true,
) {
    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = WOO_POS_ORDERS_TOOLBAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showOrderNumber) {
                WooPosShimmerText(
                    text = "Order #123",
                    style = WooPosTypography.Heading.style,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.weight(1f))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(40.dp.toAdaptiveComponentSize())
                    .width(WooPosComponentSize.XLarge.value)
                    .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosShimmerText(
            text = "Jul 28, 2025 at 10:31 PM",
            style = WooPosTypography.BodyMedium.style
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

        WooPosShimmerText(
            text = "customer@example.com",
            style = WooPosTypography.BodyMedium.style
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        WooPosShimmerBox(
            modifier = Modifier
                .width(WooPosComponentSize.Medium.value)
                .height(24.dp.toAdaptiveComponentSize())
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )

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
                        fontWeight = FontWeight.SemiBold
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
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

                    repeat(3) {
                        TotalLoadingItem()
                        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
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
    val marginSmall = WooPosSpacing.Small.value
    val marginMedium = WooPosSpacing.Medium.value

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = marginSmall)
    ) {
        val (image, nameShimmer, qtyShimmer, totalShimmer) = createRefs()

        WooPosShimmerBox(
            modifier = Modifier
                .size(56.dp.toAdaptiveIconSize())
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                .constrainAs(image) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                }
        )

        WooPosShimmerText(
            text = "Product Name",
            style = WooPosTypography.BodyLarge.style,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(nameShimmer) {
                top.linkTo(image.top)
                start.linkTo(image.end, margin = marginMedium)
            }
        )

        WooPosShimmerText(
            text = "2 x $10.00",
            style = WooPosTypography.BodyMedium.style,
            modifier = Modifier.constrainAs(qtyShimmer) {
                bottom.linkTo(image.bottom)
                start.linkTo(nameShimmer.start)
            }
        )

        WooPosShimmerText(
            text = "$20.00",
            style = WooPosTypography.BodyMedium.style,
            modifier = Modifier.constrainAs(totalShimmer) {
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
        WooPosShimmerText(
            text = "Subtotal",
            style = WooPosTypography.BodyMedium.style
        )

        WooPosShimmerText(
            text = "$100.00",
            style = WooPosTypography.BodyMedium.style
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersLoadingStatePreview() {
    WooPosTheme {
        WooPosOrdersLoadingScreen()
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersLoadingStatePhonePreview() {
    WooPosTheme {
        WooPosOrdersLoadingScreen(isPhoneLayout = true)
    }
}
