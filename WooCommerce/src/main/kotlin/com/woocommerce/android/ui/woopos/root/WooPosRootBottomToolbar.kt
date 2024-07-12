package com.woocommerce.android.ui.woopos.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
    state: State<WooPosRootScreenState>,
    onUIEvent: (WooPosRootUIEvent) -> Unit,
) {
    Card(
        modifier = Modifier
            .height(64.dp)
            .wrapContentSize(Alignment.Center),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 24.dp.toAdaptivePadding())
        ) {
            ExitPosButton { onUIEvent(WooPosRootUIEvent.ExitPOSClicked) }
            Box(
                Modifier
                    .width(0.5.dp)
                    .background(color = WooPosTheme.colors.loadingSkeleton)
            )
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
        Text(
            modifier = Modifier.padding(8.dp.toAdaptivePadding()),
            text = stringResource(id = R.string.woopos_exit_pos),
            color = MaterialTheme.colors.secondaryVariant,
            style = MaterialTheme.typography.button
        )
    }
}

@Composable
private fun CardReaderStatus(
    state: State<WooPosRootScreenState>,
    onConnectToReaderClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.value.cardReaderStatus) {
            WooPosCardReaderStatus.Connected -> {
                ReaderStatus(Color(0xFF03D479))
            }

            WooPosCardReaderStatus.NotConnected -> {
                Icon(
                    painter = painterResource(id = R.drawable.woopos_ic_reader_disconnected),
                    contentDescription = null,
                    tint = Color.Unspecified
                )
            }

            WooPosCardReaderStatus.Connecting -> {
                ReaderStatus(Color(0xFF999999))
            }

            else -> Unit
        }
        Text(
            modifier = Modifier.padding(8.dp.toAdaptivePadding()),
            text = stringResource(id = state.value.cardReaderStatus.title),
            color = MaterialTheme.colors.secondary,
            style = MaterialTheme.typography.button
        )
        when (state.value.cardReaderStatus) {
            WooPosCardReaderStatus.NotConnected -> {
                TextButton(onClick = onConnectToReaderClick) {
                    Text(text = stringResource(R.string.woopos_reader_connect_now_button))
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun ReaderStatus(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color = color, shape = CircleShape)
    )
}

@WooPosPreview
@Composable
fun PreviewWooPosBottomToolbarStatusConnecting() {
    val state = remember {
        mutableStateOf(
            WooPosRootScreenState(
                WooPosCardReaderStatus.Connected,
                null
            )
        )
    }
    WooPosTheme {
        Column {
            Spacer(modifier = Modifier.weight(1f))
            WooPosBottomToolbar(state) {}
        }
    }
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
    WooPosTheme {
        Column {
            Spacer(modifier = Modifier.weight(1f))
            WooPosBottomToolbar(state) {}
        }
    }
}

@WooPosPreview
@Composable
fun PreviewWooPosBottomToolbarStatusConnected() {
    val state = remember {
        mutableStateOf(
            WooPosRootScreenState(
                WooPosCardReaderStatus.Connecting,
                null
            )
        )
    }
    WooPosTheme {
        Column {
            Spacer(modifier = Modifier.weight(1f))
            WooPosBottomToolbar(state) {}
        }
    }
}
