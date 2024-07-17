package com.woocommerce.android.ui.woopos.root

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.root.WooPosRootScreenState.Menu.MenuItem
import com.woocommerce.android.ui.woopos.root.WooPosRootScreenState.WooPosCardReaderStatus

@Composable
fun WooPosBottomToolbar(
    modifier: Modifier = Modifier,
    state: State<WooPosRootScreenState>,
    onUIEvent: (WooPosRootUIEvent) -> Unit,
) {
    val value = state.value
    when (val menu = value.menu) {
        WooPosRootScreenState.Menu.Hidden -> ToolbarWithHiddenMenu(
            modifier = modifier,
            cardReaderStatus = value.cardReaderStatus,
            onUIEvent = onUIEvent
        )

        is WooPosRootScreenState.Menu.Visible -> ToolbarWithVisibleMenu(
            modifier = modifier,
            cardReaderStatus = value.cardReaderStatus,
            menu = menu,
            onUIEvent = onUIEvent
        )
    }
}

@Composable
fun ToolbarWithVisibleMenu(
    modifier: Modifier,
    cardReaderStatus: WooPosCardReaderStatus,
    menu: WooPosRootScreenState.Menu.Visible,
    onUIEvent: (WooPosRootUIEvent) -> Unit
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
        .clickable { onUIEvent(WooPosRootUIEvent.OnOutsideOfToolbarMenuClicked) })
    {
        ConstraintLayout(modifier = modifier) {
            val (toolbar, popupMenu) = createRefs()

            ToolbarWithHiddenMenu(
                modifier = Modifier.constrainAs(toolbar) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                },
                cardReaderStatus = cardReaderStatus,
                onUIEvent = onUIEvent
            )

            PopUpMenu(
                modifier = Modifier.constrainAs(popupMenu) {
                    bottom.linkTo(toolbar.top, margin = 8.dp)
                    start.linkTo(toolbar.start)
                },
                menuItems = menu.items,
                onClick = { menuItem ->
                    onUIEvent(WooPosRootUIEvent.MenuItemSelected(menuItem))
                }
            )
        }
    }
}

@Composable
private fun ToolbarWithHiddenMenu(
    modifier: Modifier,
    cardReaderStatus: WooPosCardReaderStatus,
    onUIEvent: (WooPosRootUIEvent) -> Unit
) {
    ConstraintLayout(modifier = modifier) {
        val (menuCard, cardReaderStatusCard) = createRefs()
        val marginBetweenCards = 8.dp.toAdaptivePadding()

        CardReaderStatusCard(
            modifier = Modifier
                .constrainAs(cardReaderStatusCard) {
                    start.linkTo(menuCard.end, margin = marginBetweenCards)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            state = cardReaderStatus,
        ) { onUIEvent(WooPosRootUIEvent.ConnectToAReaderClicked) }

        MenuCard(
            modifier = Modifier
                .constrainAs(menuCard) {
                    start.linkTo(parent.start)
                    top.linkTo(cardReaderStatusCard.top)
                    bottom.linkTo(cardReaderStatusCard.bottom)
                    height = Dimension.fillToConstraints
                }
        ) { onUIEvent(WooPosRootUIEvent.OnToolbarMenuClicked) }
    }
}

@Composable
private fun MenuCard(
    modifier: Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Circle(size = 4.dp, color = MaterialTheme.colors.primary)
                    if (it < 2) {
                        Spacer(modifier = Modifier.height(4.dp.toAdaptivePadding()))
                    }
                }
            }
        }
    }
}

@Composable
private fun PopUpMenu(
    modifier: Modifier,
    menuItems: List<MenuItem>,
    onClick: (MenuItem) -> Unit
) {
    Card(
        modifier = modifier,
        elevation = 8.dp,
    ) {
        Column {
            menuItems.forEach { menuItem ->
                PopUpMenuItem(menuItem, onClick)
            }
        }
    }
}

@Composable
private fun PopUpMenuItem(
    menuItem: MenuItem,
    onClick: (MenuItem) -> Unit
) {
    TextButton(onClick = { onClick(menuItem) }) {
        Spacer(modifier = Modifier.width(20.dp.toAdaptivePadding()))
        Icon(
            imageVector = ImageVector.vectorResource(id = menuItem.icon),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(4.dp.toAdaptivePadding()))
        Text(
            modifier = Modifier.padding(8.dp.toAdaptivePadding()),
            text = stringResource(id = menuItem.title),
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body1
        )
        Spacer(modifier = Modifier.width(20.dp.toAdaptivePadding()))
    }
}

@Composable
private fun CardReaderStatusCard(
    modifier: Modifier,
    state: WooPosCardReaderStatus,
    onClick: () -> Unit
) {
    val transition = updateTransition(
        targetState = state,
        label = "CardReaderStatusTransition"
    )

    val animationDuration = 1000
    val illustrationColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = animationDuration) },
        label = "IllustrationColorTransition"
    ) { status ->
        when (status) {
            WooPosCardReaderStatus.Connected -> WooPosTheme.colors.success
            WooPosCardReaderStatus.NotConnected -> WooPosTheme.colors.error
        }
    }

    val textColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = animationDuration) },
        label = "TextColorTransition"
    ) { status ->
        when (status) {
            WooPosCardReaderStatus.Connected -> MaterialTheme.colors.secondary
            WooPosCardReaderStatus.NotConnected -> MaterialTheme.colors.secondaryVariant.copy(
                alpha = 0.8f
            )
        }
    }

    val title = stringResource(
        id = when (state) {
            WooPosCardReaderStatus.Connected -> WooPosCardReaderStatus.Connected.title
            WooPosCardReaderStatus.NotConnected -> WooPosCardReaderStatus.NotConnected.title
        }
    )

    Card(
        modifier = modifier
            .wrapContentSize(Alignment.Center),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        TextButton(onClick = onClick) {
            Spacer(modifier = Modifier.width(16.dp.toAdaptivePadding()))
            Circle(size = 12.dp, color = illustrationColor)
            Spacer(modifier = Modifier.width(4.dp.toAdaptivePadding()))
            ReaderStatusText(
                modifier = Modifier.animateContentSize(),
                title = title,
                color = textColor,
            )
            Spacer(modifier = Modifier.width(16.dp.toAdaptivePadding()))
        }
    }
}

@Composable
private fun ReaderStatusText(
    modifier: Modifier,
    title: String,
    color: Color
) {
    Text(
        modifier = modifier.padding(8.dp.toAdaptivePadding()),
        text = title,
        color = color,
        style = MaterialTheme.typography.button
    )
}

@Composable
private fun Circle(
    size: Dp,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(color = color, shape = CircleShape)
    )
}

@WooPosPreview
@Composable
fun PreviewWooPosBottomToolbarStatusNotConnected() {
    val state = remember {
        mutableStateOf(
            WooPosRootScreenState(
                WooPosCardReaderStatus.NotConnected,
                menu = WooPosRootScreenState.Menu.Hidden,
                null
            )
        )
    }
    Preview(state)
}

@WooPosPreview
@Composable
fun PreviewWooPosBottomToolbarStatusConnectedWithMenu() {
    val state = remember {
        mutableStateOf(
            WooPosRootScreenState(
                WooPosCardReaderStatus.Connected,
                menu = WooPosRootScreenState.Menu.Visible(
                    listOf(
                        MenuItem(
                            id = 0,
                            title = R.string.woopos_exit_confirmation_title,
                            icon = R.drawable.woopos_ic_exit_pos,
                        ),
                        MenuItem(
                            id = 1,
                            title = R.string.woopos_get_support_title,
                            icon = R.drawable.woopos_ic_get_support,
                        )
                    )
                ),
                null
            )
        )
    }
    Preview(state)
}

@Composable
private fun Preview(state: MutableState<WooPosRootScreenState>) {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            WooPosBottomToolbar(
                modifier = Modifier.padding(24.dp.toAdaptivePadding()),
                state
            ) {}
        }
    }
}
