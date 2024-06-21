package com.woocommerce.android.ui.woopos.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme

@Composable
fun WooPosBottomToolbar(
    state: State<WooPosRootScreenState>,
    onUIEvent: (WooPosRootUIEvent) -> Unit,
) {
    Card(
        modifier = Modifier
            .height(60.dp)
            .fillMaxWidth(),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 8.dp,
        shape = RectangleShape
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 24.dp)
        ) {
            TextButton(onClick = { onUIEvent(WooPosRootUIEvent.ExitPOSClicked) }) {
                Text(
                    text = stringResource(id = R.string.woopos_exit_pos),
                    color = MaterialTheme.colors.secondaryVariant,
                    style = MaterialTheme.typography.button
                )
            }
            TextButton(onClick = { onUIEvent(WooPosRootUIEvent.ConnectToAReaderClicked) }) {
                Text(
                    text = stringResource(id = state.value.cardReaderStatus.title),
                    color = MaterialTheme.colors.secondary,
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
        mutableStateOf(
            WooPosRootScreenState(
                WooPosRootScreenState.WooPosCardReaderStatus.Unknown,
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
