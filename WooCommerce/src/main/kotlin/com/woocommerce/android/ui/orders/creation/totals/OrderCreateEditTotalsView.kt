package com.woocommerce.android.ui.orders.creation.totals

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooTheme
import kotlinx.coroutines.launch

@Composable
fun OrderCreateEditTotalsView(
    state: TotalsSectionsState,
    isPreview: Boolean = LocalInspectionMode.current,
) {
    WooTheme {
        val visible = state is TotalsSectionsState.Shown
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            visible = visible || isPreview,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(color = Color.Transparent),
            ) {
                val shadowHeight = dimensionResource(id = R.dimen.minor_100)
                val shadowHeightPx = with(LocalDensity.current) { shadowHeight.toPx() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.15f),
                                ),
                                startY = 0f,
                                endY = shadowHeightPx,
                            )
                        )
                        .height(shadowHeight)
                )

                if (state !is TotalsSectionsState.Shown) return@Column
                TotalsView(state)
            }
        }
    }
}

@Composable
private fun TotalsView(
    state: TotalsSectionsState.Shown,
    isPreview: Boolean = LocalInspectionMode.current,
) {
    var isExpanded by remember { mutableStateOf(isPreview) }

    val totalsIs = remember { MutableInteractionSource() }
    val topRowCoroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .background(color = colorResource(id = R.color.color_surface))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clickable(
                    interactionSource = totalsIs,
                    indication = null
                ) {
                    val press = PressInteraction.Press(Offset.Zero)
                    topRowCoroutineScope.launch {
                        totalsIs.emit(press)
                        totalsIs.emit(PressInteraction.Release(press))
                    }
                    isExpanded = !isExpanded
                }
                .animateContentSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = isExpanded,
                    label = "totals_icon",
                ) { expanded ->
                    IconButton(
                        interactionSource = totalsIs,
                        onClick = { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (expanded) {
                                Icons.Default.KeyboardArrowDown
                            } else {
                                Icons.Default.KeyboardArrowUp
                            },
                            contentDescription = null,
                            tint = colorResource(id = R.color.color_primary),
                            modifier = Modifier.padding(
                                all = dimensionResource(id = R.dimen.minor_100)
                            )
                        )
                    }
                }
            }

            if (isExpanded) {
            }
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        TotalsSummary(state)
    }
}

@Composable
fun OrderTotal() {
    RowWithData(
        title = stringResource(id = R.string.order_creation_payment_order_total),
        data = "$20.00",
        bold = true,
    )
}

@Composable
private fun TotalsSummary(state: TotalsSectionsState.Shown) {
    Divider(
        modifier = Modifier
            .padding(start = dimensionResource(id = R.dimen.major_100))
    )

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

    OrderTotal()

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

    Divider(modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100)))

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

    WCColoredButton(
        onClick = { (state as? TotalsSectionsState.Shown)?.mainButton?.onClick?.invoke() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
    ) {
        Text(
            text = (state as? TotalsSectionsState.Shown)?.mainButton?.text ?: "",
        )
    }
}

@Composable
fun RowWithData(
    title: String,
    data: String,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = dimensionResource(id = R.dimen.minor_100),
                horizontal = dimensionResource(id = R.dimen.major_100),
            )
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            color = colorResource(id = R.color.color_on_surface),
            fontWeight = if (bold) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
        Text(
            text = data,
            style = MaterialTheme.typography.body1,
            color = colorResource(id = R.color.color_on_surface),
            fontWeight = if (bold) {
                FontWeight.Bold
            } else {
                FontWeight.Normal
            }
        )
    }
}

@Composable
fun RowWithButtonAndData(
    buttonText: String,
    data: String,
    extraData: String? = null,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                end = dimensionResource(id = R.dimen.major_100),
                top = dimensionResource(id = R.dimen.minor_50),
                bottom = dimensionResource(id = R.dimen.minor_50),
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            WCTextButton(
                onClick = onClick,
                modifier = Modifier
                    .padding(vertical = 0.dp),
                contentPadding = PaddingValues(
                    vertical = 0.dp,
                    horizontal = dimensionResource(id = R.dimen.major_100),
                ),
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier
                )
            }
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))
            Text(
                text = data,
                color = colorResource(id = R.color.color_on_surface),
                style = MaterialTheme.typography.body1,
            )
        }

        extraData?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.caption,
                color = colorResource(id = R.color.color_on_surface_disabled),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(
                        start = dimensionResource(id = R.dimen.major_100),
                    )
            )
        }
    }
}

@Composable
@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
fun OrderCreateEditTotalsViewPreview() {
    OrderCreateEditTotalsView(
        state = TotalsSectionsState.Shown(
            lines = listOf(
                TotalsSectionsState.Line.Simple(
                    label = stringResource(R.string.order_creation_payment_products),
                    value = "$125.00"
                ),
                TotalsSectionsState.Line.Simple(
                    label = stringResource(R.string.custom_amounts),
                    value = "$2.00"
                ),
                TotalsSectionsState.Line.Button(
                    text = stringResource(R.string.shipping),
                    value = "$16.25",
                    onClick = {},
                ),
                TotalsSectionsState.Line.Button(
                    text = stringResource(R.string.order_creation_coupon_button),
                    value = "-$4.25",
                    extraValue = "20 OFF",
                    onClick = {},
                ),
                TotalsSectionsState.Line.Button(
                    text = stringResource(R.string.order_gift_card),
                    value = "-$4.25",
                    extraValue = "1234-5678-9987-6543",
                    onClick = {},
                ),
                TotalsSectionsState.Line.Simple(
                    label = stringResource(R.string.order_creation_payment_tax_label),
                    value = "$15.33"
                ),
                TotalsSectionsState.Line.SimpleSmall(
                    label = "Government Sales Tax · 10%",
                    value = "$12.50"
                ),
                TotalsSectionsState.Line.SimpleSmall(
                    label = "State Tax · 5%",
                    value = "$6.25"
                ),
                TotalsSectionsState.Line.LearnMore(
                    text = stringResource(R.string.order_creation_tax_based_on_billing_address),
                    buttonText = stringResource(R.string.learn_more),
                    onClick = {}
                ),
            ),
            orderTotal = TotalsSectionsState.OrderTotal(
                label = stringResource(R.string.order_creation_payment_order_total),
                value = "$143.75"
            ),
            mainButton = TotalsSectionsState.Button(
                text = "Collect Payment",
                enabled = true,
                onClick = {},
            )
        )
    )
}
