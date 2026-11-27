package com.woocommerce.android.ui.woopos.home.phone

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartPresentation
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartScreen
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartUIEvent
import kotlinx.coroutines.flow.drop
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WooPosPhoneCartSheet(
    isVisible: Boolean,
    cartState: WooPosCartState?,
    onOpenCartClick: () -> Unit,
    onDismissRequest: () -> Unit,
    onCartUIEvent: (WooPosCartUIEvent) -> Unit,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Hidden, skipHiddenState = false)
    val isOpen = isVisible && sheetState.targetValue != SheetValue.Hidden
    val isFullScreen = rememberIsFullScreen(sheetState)
    var footerHeight by remember { mutableIntStateOf(0) }
    SyncSheetWithVisibility(isVisible, sheetState, onDismissRequest)

    val isCartEmpty = cartState?.body?.amountOfItems == 0
    LaunchedEffect(isVisible, isCartEmpty) {
        if (isVisible && isCartEmpty) onDismissRequest()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val halfHeightOffset = constraints.maxHeight - with(LocalDensity.current) { (maxHeight / 2).roundToPx() }
        BottomSheetScaffold(
            scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState),
            sheetPeekHeight = maxHeight / 2,
            sheetMaxWidth = Dp.Unspecified,
            sheetShape = RoundedCornerShape(
                topStart = WooPosCornerRadius.Large.value,
                topEnd = WooPosCornerRadius.Large.value,
            ),
            sheetContainerColor = MaterialTheme.colorScheme.surfaceBright,
            sheetDragHandle = { DragHandle(isVisible = !isFullScreen) },
            containerColor = MaterialTheme.colorScheme.surface,
            sheetContent = {
                cartState?.let {
                    CartSheetContent(
                        cartState = it,
                        sheetState = sheetState,
                        halfHeightOffset = halfHeightOffset,
                        footerHeight = { footerHeight },
                        isFullScreen = isFullScreen,
                        onBack = onDismissRequest,
                        onCartUIEvent = { event -> if (!isCartEmpty) onCartUIEvent(event) },
                    )
                }
            },
        ) {
            val coveredContentModifier = when (isOpen) {
                true -> Modifier.clearAndSetSemantics {}
                false -> Modifier
            }
            Box(modifier = coveredContentModifier) {
                content()
            }
            CartSheetScrim(isOpen = isOpen, onClick = onDismissRequest)
        }

        WooPosHomePhonePersistentButton(
            state = phonePersistentButtonState(cartState, isSheetOpen = isOpen),
            onOpenCartClick = onOpenCartClick,
            onCheckoutClick = { onCartUIEvent(WooPosCartUIEvent.CheckoutClicked) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { footerHeight = it.height },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncSheetWithVisibility(isVisible: Boolean, sheetState: SheetState, onDismissRequest: () -> Unit) {
    LaunchedEffect(isVisible) {
        if (isVisible) sheetState.partialExpand() else sheetState.hide()
    }
    val currentIsVisible by rememberUpdatedState(isVisible)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.currentValue }
            .drop(1)
            .collect { if (it == SheetValue.Hidden && currentIsVisible) currentOnDismissRequest() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberIsFullScreen(sheetState: SheetState): Boolean {
    var isFullScreen by remember { mutableStateOf(false) }
    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.targetValue to sheetState.currentValue }.collect { (target, current) ->
            when {
                target != SheetValue.Hidden -> isFullScreen = target == SheetValue.Expanded
                current == SheetValue.Hidden -> isFullScreen = false
            }
        }
    }
    return isFullScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartSheetContent(
    cartState: WooPosCartState,
    sheetState: SheetState,
    halfHeightOffset: Int,
    footerHeight: () -> Int,
    isFullScreen: Boolean,
    onBack: () -> Unit,
    onCartUIEvent: (WooPosCartUIEvent) -> Unit,
) {
    var shownCartState by remember { mutableStateOf(cartState) }
    if (cartState.body.amountOfItems > 0) shownCartState = cartState
    val statusBars = WindowInsets.statusBars
    val density = LocalDensity.current
    val fullScreenTopMargin = animateDpAsState(
        targetValue = when (isFullScreen) {
            true -> WooPosSpacing.Large.value
            false -> 0.dp
        },
        label = "cart_sheet_top_margin",
    )

    WooPosCartScreen(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val sheetOffset = runCatching { sheetState.requireOffset() }
                    .getOrDefault(constraints.maxHeight.toFloat())
                    .roundToInt()
                    .coerceAtMost(halfHeightOffset)
                val topInset = fullScreenTopMargin.value.roundToPx() +
                    (statusBars.getTop(density) - sheetOffset).coerceAtLeast(0)
                val visibleHeight = (constraints.maxHeight - sheetOffset - footerHeight() - topInset)
                    .coerceIn(0, constraints.maxHeight)
                val placeable = measurable.measure(
                    constraints.copy(minHeight = visibleHeight, maxHeight = visibleHeight)
                )
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, topInset) }
            }
            .consumeWindowInsets(statusBars),
        state = shownCartState,
        onUIEvent = onCartUIEvent,
        presentation = WooPosCartPresentation.PhoneSheet(isFullScreen = isFullScreen, onBack = onBack),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DragHandle(isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        BottomSheetDefaults.DragHandle()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartSheetScrim(isOpen: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = SCRIM_STIFFNESS),
        label = "cart_sheet_scrim",
    )
    val color = BottomSheetDefaults.ScrimColor
    val currentOnClick by rememberUpdatedState(onClick)
    val tapModifier = when (isOpen) {
        true -> Modifier.pointerInput(Unit) { detectTapGestures { currentOnClick() } }
        false -> Modifier
    }
    Canvas(modifier = Modifier.fillMaxSize().then(tapModifier)) {
        drawRect(color = color, alpha = alpha.coerceIn(0f, 1f))
    }
}

private const val SCRIM_STIFFNESS = 1600f
