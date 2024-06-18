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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel

@Composable
fun OrderCreateEditTotalsView(viewModel: OrderCreateEditViewModel) {
    viewModel.totalsData.observeAsState().value?.let { state ->
        OrderCreateEditTotalsView(
            state,
            modifier = Modifier.onGloballyPositioned {
                state.onHeightChanged(it.size.height)
            }
        )
    }
}

@Composable
private fun OrderCreateEditTotalsView(
    state: TotalsSectionsState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state is TotalsSectionsState.Full,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        if (state is TotalsSectionsState.Full) {
            OrderCreateEditTotalsFullView(state, modifier)
        }
    }

    AnimatedVisibility(
        visible = state is TotalsSectionsState.Minimised,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        if (state is TotalsSectionsState.Minimised) {
            OrderCreateEditTotalsMinimisedView(state, modifier)
        }
    }
}

@Composable
private fun OrderCreateEditTotalsFullView(
    state: TotalsSectionsState.Full,
    modifier: Modifier = Modifier
) {
    PanelWithShadow {
        TotalsView(
            state,
            modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { state.onExpandCollapseClicked() }
        )
    }
}

@Composable
private fun OrderCreateEditTotalsMinimisedView(
    state: TotalsSectionsState.Minimised,
    modifier: Modifier = Modifier
) {
    PanelWithShadow {
        Column(
            modifier = modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { state.onExpandCollapseClicked() }
                .background(color = colorResource(id = R.color.color_surface))
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            RowWithData(
                title = state.orderTotal.label,
                data = state.orderTotal.value,
                bold = true,
            )
            if (state.recalculateButton != null) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                WCColoredButton(
                    onClick = state.recalculateButton.onClick,
                    enabled = state.recalculateButton.enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                ) {
                    Text(
                        text = state.recalculateButton.text,
                    )
                }
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }
}

@Composable
private fun PanelWithShadow(content: @Composable ColumnScope.() -> Unit) {
    WooTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(color = Color.Transparent)
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

            content()
        }
    }
}

@Composable
private fun TotalsView(
    state: TotalsSectionsState.Full,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color = colorResource(id = R.color.color_surface))
            .verticalScroll(rememberScrollState())
            .onGloballyPositioned {
                state.onHeightChanged(it.size.height)
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .animateContentSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(
                    targetState = state.isExpanded,
                    label = "totals_icon",
                ) { expanded ->
                    Icon(
                        modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                        painter = if (expanded) {
                            painterResource(R.drawable.ic_arrow_down_26)
                        } else {
                            painterResource(R.drawable.ic_arrow_up_26)
                        },
                        contentDescription = stringResource(R.string.order_creation_expand_collapse_order_totals),
                        tint = colorResource(id = R.color.color_primary),
                    )
                }
            }

            if (state.isExpanded) {
                Lines(lines = state.lines, smallGaps = false)

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                Divider(modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100)))

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            }
        }

        TotalsSummary(state.orderTotal, state.mainButton)
    }
}

@Composable
private fun Lines(lines: List<TotalsSectionsState.Line>, smallGaps: Boolean) {
    lines.forEachIndexed { index, line ->
        if (index != 0) LineGap(smallGaps)

        when (line) {
            is TotalsSectionsState.Line.Simple -> RowWithData(
                title = line.label,
                data = line.value,
            )

            is TotalsSectionsState.Line.SimpleSmall -> RowWithDataSmall(line)

            is TotalsSectionsState.Line.Button -> RowWithButtonAndData(line)

            is TotalsSectionsState.Line.Block -> {
                Lines(lines = line.lines, smallGaps = true)
            }

            is TotalsSectionsState.Line.LearnMore -> LearnMore(line)
        }

        if (index != lines.size - 1) LineGap(smallGaps)
    }
}

@Composable
private fun LineGap(smallGaps: Boolean) {
    if (smallGaps) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_25)))
    } else {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
    }
}

@Composable
private fun TotalsSummary(
    orderTotal: TotalsSectionsState.OrderTotal,
    mainButton: TotalsSectionsState.Button,
) {
    RowWithData(
        title = orderTotal.label,
        data = orderTotal.value,
        bold = true,
    )

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

    Divider(modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100)))

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

    WCColoredButton(
        onClick = mainButton.onClick,
        enabled = mainButton.enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
    ) {
        Text(
            text = mainButton.text,
        )
    }

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
}

@Composable
private fun LearnMore(learnMore: TotalsSectionsState.Line.LearnMore) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.major_75),
                end = dimensionResource(id = R.dimen.major_100),
            )
            .wrapContentHeight()
            .clickable(onClick = learnMore.onClick)
            .padding(all = dimensionResource(id = R.dimen.minor_50)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = learnMore.text,
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium),
        )
        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = learnMore.buttonText,
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_primary),
        )
    }
}

@Composable
private fun RowWithData(
    title: String,
    data: String,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
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
private fun RowWithDataSmall(lineSimpleSmall: TotalsSectionsState.Line.SimpleSmall) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            .wrapContentHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = lineSimpleSmall.label,
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = lineSimpleSmall.value,
            style = MaterialTheme.typography.caption,
            color = colorResource(id = R.color.color_on_surface_medium),
        )
    }
}

@Composable
private fun RowWithButtonAndData(lineWithButton: TotalsSectionsState.Line.Button) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(id = R.dimen.major_75),
                end = dimensionResource(id = R.dimen.major_100),
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextWithIcon(
                text = lineWithButton.text,
                isEnabled = lineWithButton.enabled,
                modifier = Modifier
                    .clickable { if (lineWithButton.enabled) lineWithButton.onClick() }
                    .padding(all = dimensionResource(id = R.dimen.minor_50))
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))
            Text(
                text = lineWithButton.value,
                color = colorResource(id = R.color.color_on_surface),
                style = MaterialTheme.typography.body1,
            )
        }

        lineWithButton.extraValue?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.caption,
                color = colorResource(id = R.color.color_on_surface_medium),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(start = dimensionResource(id = R.dimen.minor_50))
            )
        }
    }
}

@Composable
private fun TextWithIcon(
    text: String,
    isEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val pencilId = "pencil"
    val inlineContent = mapOf(
        Pair(
            pencilId,
            InlineTextContent(
                Placeholder(
                    width = 20.sp,
                    height = 20.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextBottom
                )
            ) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.Edit else Icons.Default.Lock,
                    contentDescription = null,
                    tint = colorResource(id = R.color.color_primary),
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    )
    Text(
        inlineContent = inlineContent,
        style = MaterialTheme.typography.subtitle1,
        color = if (isEnabled) {
            colorResource(id = R.color.color_primary)
        } else {
            colorResource(id = R.color.color_on_surface)
        },
        text = buildAnnotatedString {
            append(text)
            append("  ")
            appendInlineContent(pencilId)
        },
        modifier = modifier,
    )
}

@Composable
@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OrderCreateEditTotalsFullViewPreview() {
    OrderCreateEditTotalsFullView(
        state = TotalsSectionsState.Full(
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
                    enabled = true,
                    onClick = {},
                ),
                TotalsSectionsState.Line.Button(
                    text = stringResource(R.string.order_creation_coupon_button),
                    value = "-$4.25",
                    extraValue = "20 OFF",
                    enabled = false,
                    onClick = {},
                ),
                TotalsSectionsState.Line.Button(
                    text = stringResource(R.string.order_gift_card),
                    value = "-$4.25",
                    extraValue = "1234-5678-9987-6543",
                    enabled = false,
                    onClick = {},
                ),
                TotalsSectionsState.Line.Block(
                    lines = listOf(
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
                        )
                    )
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
            ),
            isExpanded = true,
            onExpandCollapseClicked = {},
            onHeightChanged = {},
        )
    )
}

@Composable
@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun OrderCreateEditTotalsMinimisedViewPreview() {
    OrderCreateEditTotalsMinimisedView(
        state = TotalsSectionsState.Minimised(
            orderTotal = TotalsSectionsState.OrderTotal(
                label = stringResource(R.string.order_creation_payment_order_total),
                value = "$143.75"
            ),
            onHeightChanged = {},
            onExpandCollapseClicked = {}
        )
    )
}
