package com.woocommerce.android.ui.woopos.root

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.root.WooPosRootScreenState.WooPosCardReaderStatus

@Composable
fun WooPosBottomToolbar(
    modifier: Modifier = Modifier,
    state: State<WooPosRootScreenState>,
    onUIEvent: (WooPosRootUIEvent) -> Unit,
) {
    Card(
        modifier = modifier
            .wrapContentSize(Alignment.Center),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                horizontal = 24.dp.toAdaptivePadding(),
                vertical = 8.dp.toAdaptivePadding()
            )
        ) {
            ExitPosButton { onUIEvent(WooPosRootUIEvent.ExitPOSClicked) }
            Spacer(modifier = Modifier.width(8.dp.toAdaptivePadding()))
            DividerVertical()
            Spacer(modifier = Modifier.width(8.dp.toAdaptivePadding()))
            CardReaderStatus(state) { onUIEvent(WooPosRootUIEvent.ConnectToAReaderClicked) }
        }
    }
}

@Composable
private fun ExitPosButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.woopos_ic_exit_pos),
            contentDescription = null,
            tint = MaterialTheme.colors.secondaryVariant,
        )
        Spacer(modifier = Modifier.width(8.dp.toAdaptivePadding()))
        Text(
            text = stringResource(id = R.string.woopos_exit_pos),
            color = MaterialTheme.colors.secondaryVariant,
            style = MaterialTheme.typography.button
        )
    }
}

@Composable
private fun CardReaderStatus(
    state: State<WooPosRootScreenState>,
    onClick: () -> Unit
) {
    val transition = updateTransition(
        targetState = state.value.cardReaderStatus,
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
        id = when (state.value.cardReaderStatus) {
            WooPosCardReaderStatus.Connected -> WooPosCardReaderStatus.Connected.title
            WooPosCardReaderStatus.NotConnected -> WooPosCardReaderStatus.NotConnected.title
        }
    )

    TextButton(onClick = onClick) {
        ReaderStatusIllustration(illustrationColor)
        Spacer(modifier = Modifier.width(4.dp.toAdaptivePadding()))
        ReaderStatusText(
            modifier = Modifier.animateContentSize(),
            title = title,
            color = textColor,
        )
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
private fun ReaderStatusIllustration(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color = color, shape = CircleShape)
    )
}

@Composable
private fun DividerVertical() {
    Box(
        Modifier
            .width(0.5.dp)
            .height(24.dp)
            .background(color = WooPosTheme.colors.loadingSkeleton)
    )
}

@WooPosPreview
@Composable
fun PreviewWooPosBottomToolbarStatusNotConnected() {
    val state = remember {
        mutableStateOf(
            WooPosRootScreenState(
                WooPosCardReaderStatus.NotConnected,
                null
            )
        )
    }
    Preview(state)
}

@WooPosPreview
@Composable
fun PreviewWooPosBottomToolbarStatusConnected() {
    val state = remember {
        mutableStateOf(
            WooPosRootScreenState(
                WooPosCardReaderStatus.Connected,
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
