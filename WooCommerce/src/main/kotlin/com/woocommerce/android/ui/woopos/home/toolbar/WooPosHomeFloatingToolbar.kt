package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackgroundOverlay
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarState.Menu
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosHomeFloatingToolbarState.WooPosCardReaderStatus

private val TOOLBAR_ELEVATION = WooPosElevation.Medium

@Composable
fun WooPosFloatingToolbar(modifier: Modifier = Modifier, isCompact: Boolean = false) {
    val viewModel: WooPosHomeFloatingToolbarViewModel = hiltViewModel()
    WooPosFloatingToolbar(
        modifier = modifier,
        state = viewModel.state.collectAsState(),
        isCompact = isCompact,
    ) { uiEvent ->
        viewModel.onUiEvent(uiEvent)
    }
}

@Composable
private fun WooPosFloatingToolbar(
    modifier: Modifier = Modifier,
    state: State<WooPosHomeFloatingToolbarState>,
    isCompact: Boolean = false,
    onUIEvent: (WooPosHomeFloatingToolbarUIEvent) -> Unit,
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
            onClick = { onUIEvent(WooPosHomeFloatingToolbarUIEvent.OnOutsideOfToolbarMenuClicked) }
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
                        isCompact = isCompact,
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
                        isCompact = isCompact,
                        onUIEvent = onUIEvent
                    )

                    val marginBetweenCards = WooPosSpacing.Small.value
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
                            onUIEvent(WooPosHomeFloatingToolbarUIEvent.MenuItemClicked(menuItem))
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
    isCompact: Boolean = false,
    onUIEvent: (WooPosHomeFloatingToolbarUIEvent) -> Unit
) {
    val labels = getToolbarAccessibilityLabels(cardReaderStatus, menuCardDisabled)

    ConstraintLayout(modifier = modifier) {
        val (menuCard, cardReaderStatusCard) = createRefs()
        val marginBetweenCards = WooPosSpacing.Small.value

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
            menuCardDisabled = menuCardDisabled,
            isCompact = isCompact,
        ) { onUIEvent(WooPosHomeFloatingToolbarUIEvent.OnCardReaderStatusClicked) }

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
            isCompact = isCompact,
        ) { onUIEvent(WooPosHomeFloatingToolbarUIEvent.OnToolbarMenuClicked) }
    }
}

@Composable
private fun MenuButtonWithPopUpMenu(
    modifier: Modifier,
    menuCardDisabled: Boolean,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    val menuContentDescription = stringResource(id = R.string.woopos_menu_toolbar_content_description)
    val buttonSize = if (isCompact) 56.dp else 80.dp
    WooPosCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
        elevation = TOOLBAR_ELEVATION,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    ) {
        TextButton(
            modifier = Modifier
                .semantics { contentDescription = menuContentDescription }
                .size(buttonSize),
            onClick = onClick,
            contentPadding = PaddingValues(WooPosSpacing.None.value),
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (menuCardDisabled) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(3) {
                    Circle(size = 6.dp, color = MaterialTheme.colorScheme.onSurface)
                    if (it < 2) {
                        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))
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
    WooPosCard(
        modifier = modifier.width(IntrinsicSize.Max),
        elevation = TOOLBAR_ELEVATION,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
            menuItems.forEach { menuItem ->
                PopUpMenuItem(menuItem, onClick)
            }
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
        }
    }
}

@Composable
private fun PopUpMenuItem(
    menuItem: Menu.MenuItem,
    onClick: (Menu.MenuItem) -> Unit
) {
    TextButton(onClick = { onClick(menuItem) }) {
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
        Icon(
            imageVector = ImageVector.vectorResource(menuItem.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
        WooPosText(
            modifier = Modifier
                .padding(vertical = WooPosSpacing.Small.value)
                .weight(1f),
            text = stringResource(id = menuItem.title),
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value))
    }
}

@Composable
private fun CardReaderStatusButton(
    modifier: Modifier,
    state: WooPosCardReaderStatus,
    menuCardDisabled: Boolean,
    isCompact: Boolean = false,
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
            WooPosCardReaderStatus.NotConnected -> WooPosTheme.colors.alert
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
            WooPosCardReaderStatus.NotConnected -> MaterialTheme.colorScheme.primary
        }
    }

    val cardSize = if (isCompact) 56.dp else 80.dp
    WooPosCard(
        modifier = modifier
            .height(cardSize)
            .then(if (isCompact) Modifier.width(cardSize) else Modifier),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
        elevation = TOOLBAR_ELEVATION,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    ) {
        Surface(
            color = if (menuCardDisabled) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                Color.Transparent
            },
        ) {
            TextButton(
                onClick = onClick,
                modifier = Modifier
                    .padding(WooPosSpacing.Small.value)
                    .then(
                        if (!isCompact) {
                            Modifier.border(
                                width = 2.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(WooPosCornerRadius.Small.value),
                            )
                        } else {
                            Modifier
                        }
                    )
                    .height(40.dp),
            ) {
                Spacer(
                    modifier = Modifier.width(
                        if (isCompact) WooPosSpacing.Small.value else WooPosSpacing.Medium.value
                    )
                )
                Circle(size = 14.dp, color = illustrationColor)
                if (!isCompact) {
                    Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
                    ReaderStatusText(
                        modifier = Modifier.animateContentSize(),
                        title = title,
                    )
                }
                Spacer(
                    modifier = Modifier.width(
                        if (isCompact) WooPosSpacing.Small.value else WooPosSpacing.Medium.value
                    )
                )
            }
        }
    }
}

@Composable
private fun ReaderStatusText(
    modifier: Modifier,
    title: String,
) {
    WooPosText(
        modifier = modifier.padding(horizontal = WooPosSpacing.Small.value),
        text = title,
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
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
): WooPosHomeFloatingToolbarAccessibilityLabels {
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

    return WooPosHomeFloatingToolbarAccessibilityLabels(
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
            WooPosHomeFloatingToolbarState(
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
            WooPosHomeFloatingToolbarState(
                cardReaderStatus = WooPosCardReaderStatus.Connected,
                menu = Menu.Visible(
                    listOf(
                        Menu.MenuItem(
                            title = R.string.woopos_orders_title,
                            icon = R.drawable.ic_description_filled_24dp
                        ),
                        Menu.MenuItem(
                            title = R.string.woopos_settings_title,
                            icon = R.drawable.ic_settings_filled_24dp,
                        ),
                        Menu.MenuItem(
                            title = R.string.woopos_exit_confirmation_title,
                            icon = R.drawable.ic_exit_to_app_24dp,
                        ),
                    )
                ),
            )
        )
    }
    Preview(state)
}

@Composable
private fun Preview(state: MutableState<WooPosHomeFloatingToolbarState>) {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            WooPosFloatingToolbar(
                modifier = Modifier
                    .padding(WooPosSpacing.Large.value)
                    .align(Alignment.BottomStart),
                state
            ) {}
        }
    }
}
