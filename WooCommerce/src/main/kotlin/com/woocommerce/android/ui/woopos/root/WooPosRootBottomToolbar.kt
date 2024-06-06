package com.woocommerce.android.ui.woopos.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme

@Composable
fun WooPosBottomToolbar(state: State<WooPosBottomToolbarState>, onUIEvent: (WooPosRootUIEvent) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        color = MaterialTheme.colors.background
    ) {
        Row(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { onUIEvent(WooPosRootUIEvent.ExitPOSClicked) }) {
                Text(
                    text = stringResource(id = R.string.woopos_exit_pos),
                    color = Color.White,
                    style = MaterialTheme.typography.button
                )
            }
            TextButton(onClick = { onUIEvent(WooPosRootUIEvent.ConnectToAReaderClicked) }) {
                Text(
                    text = stringResource(id = state.value.cardReaderStatus.title),
                    color = MaterialTheme.colors.secondaryVariant,
                    style = MaterialTheme.typography.button
                )
            }
        }
    }
}

@WooPosPreview
@Composable
fun PreviewWooPosBottomToolbar() {
    val state = remember {
        mutableStateOf(WooPosBottomToolbarState(WooPosBottomToolbarState.CardReaderStatus.Unknown))
    }
    WooPosTheme {
        Column {
            Spacer(modifier = Modifier.weight(1f))
            WooPosBottomToolbar(state) {}
        }
    }
}
