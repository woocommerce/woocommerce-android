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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackgroundOverlay
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarState.Menu
import com.woocommerce.android.ui.woopos.home.toolbar.WooPosToolbarState.WooPosCardReaderStatus
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.coroutines.flow.collectLatest

private val TOOLBAR_ELEVATION = WooPosElevation.Medium

@Composable
fun WooPosFloatingToolbar(modifier: Modifier = Modifier) {
    val viewModel: WooPosToolbarViewModel = hiltViewModel()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.openUrlEvent.collectLatest { url ->
            ChromeCustomTabUtils.launchUrl(context, url, enableSlideAnimation = true)
        }
    }
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

                    val marginBetweenCards = WooPosSpacing.Small.value.toAdaptivePadding()
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
        val marginBetweenCards = WooPosSpacing.Small.value.toAdaptivePadding()

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
    WooPosCard(
        modifier = modifier,
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
        elevation = TOOLBAR_ELEVATION,
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
    ) {
        TextButton(
            modifier = Modifier.semantics { contentDescription = menuContentDescription }
                .size(80.dp),
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
                        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value.toAdaptivePadding()))
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
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
            menuItems.forEach { menuItem ->
                PopUpMenuItem(menuItem, onClick)
            }
            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))
        }
    }
}

@Composable
private fun PopUpMenuItem(
    menuItem: Menu.MenuItem,
    onClick: (Menu.MenuItem) -> Unit
) {
    TextButton(onClick = { onClick(menuItem) }) {
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))
        Icon(
            imageVector = ImageVector.vectorResource(id = menuItem.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(WooPosSpacing.Large.value)
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))
        WooPosText(
            modifier = Modifier
                .padding(vertical = WooPosSpacing.Small.value.toAdaptivePadding())
                .weight(1f),
            text = stringResource(id = menuItem.title),
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))
    }
}

@Composable
private fun CardReaderStatusButton(
    modifier: Modifier,
    state: WooPosCardReaderStatus,
    menuCardDisabled: Boolean,
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

    WooPosCard(
        modifier = modifier
            .height(80.dp),
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
                    .padding(WooPosSpacing.Small.value.toAdaptivePadding())
                    .border(
                        width = 2.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(WooPosCornerRadius.Small.value),
                    )
                    .height(40.dp),
            ) {
                Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))
                Circle(size = 14.dp, color = illustrationColor)
                Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value.toAdaptivePadding()))
                ReaderStatusText(
                    modifier = Modifier.animateContentSize(),
                    title = title,
                )
                Spacer(modifier = Modifier.width(WooPosSpacing.Medium.value.toAdaptivePadding()))
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
        modifier = modifier.padding(horizontal = WooPosSpacing.Small.value.toAdaptivePadding()),
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
                            title = R.string.woopos_documentation_title,
                            icon = R.drawable.woo_pos_info_ic
                        ),
                        Menu.MenuItem(
                            title = R.string.woopos_exit_confirmation_title,
                            icon = R.drawable.ic_woo_pos_exit,
                        ),
                        Menu.MenuItem(
                            title = R.string.woopos_get_support_title,
                            icon = R.drawable.woopos_ic_get_support,
                        ),
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
                    .padding(WooPosSpacing.Large.value.toAdaptivePadding())
                    .align(Alignment.BottomStart),
                state
            ) {}
        }
    }
}
