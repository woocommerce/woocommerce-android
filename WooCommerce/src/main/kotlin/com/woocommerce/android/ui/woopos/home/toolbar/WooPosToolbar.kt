package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackgroundOverlay
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarState.Menu
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarState.WooPosCardReaderStatus

@Composable
fun WooPosFloatingToolbar(modifier: Modifier = Modifier) {
    val viewModel: WooPosToolbarViewModel = hiltViewModel()
    WooPosFloatingToolbar(
        modifier = modifier,
        state = viewModel.state.collectAsState(),
    ) { uiEvent ->
        viewModel.onUiEvent(uiEvent)
    }
}

@Composable
private fun WooPosFloatingToolbar(
    modifier: Modifier = Modifier,
    state: State<WooPosToolbarState>,
    onUIEvent: (WooPosToolbarUIEvent) -> Unit,
) {
    val cardReaderStatus = state.value.cardReaderStatus
    val menu = state.value.menu

    val labels = getToolbarAccessibilityLabels(
        cardReaderStatus = cardReaderStatus,
        menuCardDisabled = menu is Menu.Visible
    )

    Box(modifier = Modifier.fillMaxSize()) {
        WooPosBackgroundOverlay(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = labels.floatingToolbarMenuOverlayContentDescription
                },
            isVisible = menu is Menu.Visible,
            onClick = { onUIEvent(WooPosToolbarUIEvent.OnOutsideOfToolbarMenuClicked) }
        )

        ConstraintLayout(modifier = modifier) {
            val (toolbar, popupMenu) = createRefs()

            when (menu) {
                is Menu.Hidden -> {
                    Toolbar(
                        modifier = Modifier
                            .constrainAs(toolbar) {
                                bottom.linkTo(parent.bottom)
                                start.linkTo(parent.start)
                            },
                        cardReaderStatus = cardReaderStatus,
                        menuCardDisabled = false,
                        onUIEvent = onUIEvent
                    )
                }

                is Menu.Visible -> {
                    Toolbar(
                        modifier = Modifier
                            .constrainAs(toolbar) {
                                bottom.linkTo(parent.bottom)
                                start.linkTo(parent.start)
                            },
                        cardReaderStatus = cardReaderStatus,
                        menuCardDisabled = true,
                        onUIEvent = onUIEvent
                    )

                    val marginBetweenCards = 8.dp.toAdaptivePadding()
                    PopUpMenu(
                        modifier = Modifier
                            .constrainAs(popupMenu) {
                                bottom.linkTo(toolbar.top, margin = marginBetweenCards)
                                start.linkTo(toolbar.start)
                            }
                            .semantics {
                                contentDescription = labels.floatingToolbarPopUpMenuOpenContentDescription
                            },
                        menuItems = menu.items,
                        onClick = { menuItem ->
                            onUIEvent(WooPosToolbarUIEvent.MenuItemClicked(menuItem))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Toolbar(
    modifier: Modifier,
    menuCardDisabled: Boolean,
    cardReaderStatus: WooPosCardReaderStatus,
    onUIEvent: (WooPosToolbarUIEvent) -> Unit
) {
    val labels = getToolbarAccessibilityLabels(cardReaderStatus, menuCardDisabled)

    ConstraintLayout(modifier = modifier) {
        val (menuCard, cardReaderStatusCard) = createRefs()
        val marginBetweenCards = 8.dp.toAdaptivePadding()

        CardReaderStatusButton(
            modifier = Modifier
                .constrainAs(cardReaderStatusCard) {
                    start.linkTo(menuCard.end, margin = marginBetweenCards)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
                .semantics {
                    contentDescription = labels.cardReaderStatusContentDescription
                },
            state = cardReaderStatus,
        ) { onUIEvent(WooPosToolbarUIEvent.OnCardReaderStatusClicked) }

        MenuButtonWithPopUpMenu(
            modifier = Modifier
                .constrainAs(menuCard) {
                    start.linkTo(parent.start)
                    top.linkTo(cardReaderStatusCard.top)
                    bottom.linkTo(cardReaderStatusCard.bottom)
                    height = Dimension.fillToConstraints
                }
                .semantics {
                    contentDescription = if (menuCardDisabled) {
                        labels.floatingToolbarPopUpMenuOpenContentDescription
                    } else {
                        labels.floatingToolbarPopUpMenuContentDescription
                    }
                    stateDescription = labels.floatingToolbarPopUpMenuStateDescription
                },
            menuCardDisabled = menuCardDisabled,
        ) { onUIEvent(WooPosToolbarUIEvent.OnToolbarMenuClicked) }
    }
}

@Composable
private fun MenuButtonWithPopUpMenu(
    modifier: Modifier,
    menuCardDisabled: Boolean,
    onClick: () -> Unit
) {
    val menuContentDescription = stringResource(id = R.string.woopos_menu_toolbar_content_description)
    Card(
        modifier = modifier,
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        TextButton(
            modifier = Modifier.semantics { contentDescription = menuContentDescription },
            onClick = onClick,
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = if (menuCardDisabled) {
                    MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colors.surface
                }
            )
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Circle(size = 4.dp, color = MaterialTheme.colors.onSurface)
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
    menuItems: List<Menu.MenuItem>,
    onClick: (Menu.MenuItem) -> Unit
) {
    Card(
        modifier = modifier.width(214.dp),
        elevation = 8.dp,
    ) {
        Column {
            Spacer(modifier = Modifier.height(8.dp.toAdaptivePadding()))
            menuItems.forEach { menuItem ->
                PopUpMenuItem(menuItem, onClick)
            }
            Spacer(modifier = Modifier.height(8.dp.toAdaptivePadding()))
        }
    }
}

@Composable
private fun PopUpMenuItem(
    menuItem: Menu.MenuItem,
    onClick: (Menu.MenuItem) -> Unit
) {
    TextButton(onClick = { onClick(menuItem) }) {
        Spacer(modifier = Modifier.width(20.dp.toAdaptivePadding()))
        Icon(
            imageVector = ImageVector.vectorResource(id = menuItem.icon),
            contentDescription = null,
            tint = MaterialTheme.colors.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp.toAdaptivePadding()))
        Text(
            modifier = Modifier
                .padding(vertical = 8.dp.toAdaptivePadding())
                .weight(1f),
            text = stringResource(id = menuItem.title),
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(20.dp.toAdaptivePadding()))
    }
}

@Composable
private fun CardReaderStatusButton(
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
            WooPosCardReaderStatus.Connected -> MaterialTheme.colors.onSurface
            WooPosCardReaderStatus.NotConnected -> MaterialTheme.colors.primary
        }
    }

    val title = stringResource(
        id = when (state) {
            WooPosCardReaderStatus.Connected -> WooPosCardReaderStatus.Connected.title
            WooPosCardReaderStatus.NotConnected -> WooPosCardReaderStatus.NotConnected.title
        }
    )

    val borderColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = animationDuration) },
        label = "BorderColorTransition"
    ) { status ->
        when (status) {
            WooPosCardReaderStatus.Connected -> Color.Transparent
            WooPosCardReaderStatus.NotConnected -> MaterialTheme.colors.primary
        }
    }

    Card(
        modifier = modifier
            .height(56.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .padding(8.dp.toAdaptivePadding())
                .border(
                    width = 2.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .height(40.dp),
        ) {
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
        modifier = modifier.padding(horizontal = 8.dp.toAdaptivePadding()),
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

@Composable
private fun getToolbarAccessibilityLabels(
    cardReaderStatus: WooPosCardReaderStatus,
    menuCardDisabled: Boolean
): ToolbarAccessibilityLabels {
    val floatingToolbarPopUpMenuOpenContentDescription = stringResource(
        id = R.string.woopos_floating_toolbar_pop_up_menu_open_content_description
    )

    val cardReaderStatusContentDescription = when (cardReaderStatus) {
        WooPosCardReaderStatus.Connected -> stringResource(
            id = R.string.woopos_floating_toolbar_card_reader_connected_status_content_description
        )
        WooPosCardReaderStatus.NotConnected -> stringResource(
            id = R.string.woopos_floating_toolbar_card_reader_not_connected_status_content_description
        )
    }
    val floatingToolbarMenuOverlayContentDescription = when (menuCardDisabled) {
        true -> {
            stringResource(id = R.string.woopos_floating_toolbar_overlay_menu_content_description)
        }
        false -> {
            ""
        }
    }

    val floatingToolbarPopUpMenuContentDescription = when (menuCardDisabled) {
        true -> {
            stringResource(
                id = R.string.woopos_floating_toolbar_pop_up_menu_open_content_description
            )
        }
        false -> {
            stringResource(
                id = R.string.woopos_floating_toolbar_pop_up_menu_content_description
            )
        }
    }

    val floatingToolbarPopUpMenuStateDescription = when (menuCardDisabled) {
        true -> {
            stringResource(
                id = R.string.woopos_floating_toolbar_menu_disabled_content_description
            )
        }
        false -> {
            stringResource(
                id = R.string.woopos_floating_toolbar_menu_enabled_content_description
            )
        }
    }

    return ToolbarAccessibilityLabels(
        cardReaderStatusContentDescription = cardReaderStatusContentDescription,
        floatingToolbarPopUpMenuStateDescription = floatingToolbarPopUpMenuStateDescription,
        floatingToolbarMenuOverlayContentDescription = floatingToolbarMenuOverlayContentDescription,
        floatingToolbarPopUpMenuContentDescription = floatingToolbarPopUpMenuContentDescription,
        floatingToolbarPopUpMenuOpenContentDescription = floatingToolbarPopUpMenuOpenContentDescription
    )
}

@WooPosPreview
@Composable
fun PreviewWooPosFloatingToolbarStatusNotConnected() {
    val state = remember {
        mutableStateOf(
            WooPosToolbarState(
                cardReaderStatus = WooPosCardReaderStatus.NotConnected,
                menu = Menu.Hidden
            )
        )
    }
    Preview(state)
}

@WooPosPreview
@Composable
fun PreviewWooPosFloatingToolbarStatusConnectedWithMenu() {
    val state = remember {
        mutableStateOf(
            WooPosToolbarState(
                cardReaderStatus = WooPosCardReaderStatus.Connected,
                menu = Menu.Visible(
                    listOf(
                        Menu.MenuItem(
                            title = R.string.woopos_exit_confirmation_title,
                            icon = R.drawable.woopos_ic_exit_pos,
                        ),
                        Menu.MenuItem(
                            title = R.string.woopos_get_support_title,
                            icon = R.drawable.woopos_ic_get_support,
                        )
                    )
                ),
            )
        )
    }
    Preview(state)
}

@Composable
private fun Preview(state: MutableState<WooPosToolbarState>) {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            WooPosFloatingToolbar(
                modifier = Modifier
                    .padding(24.dp.toAdaptivePadding())
                    .align(Alignment.BottomStart),
                state
            ) {}
        }
    }
}
