package com.woocommerce.android.ui.orders.creation.totals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun OrderCreateEditTotalsView(state: TotalsSectionsState.Shown) {
    WooThemeWithBackground {
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colors.surface)
                .fillMaxWidth()
                .wrapContentHeight()
                .shadow(1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = null,
                tint = colorResource(id = R.color.color_primary)
            )

            Spacer(modifier = Modifier.height(24.dp))

            WCColoredButton(
                onClick = { state.button.onClick },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            ) {
                Text(text = state.button.text)
            }
        }
    }
}

@Composable
@Preview
fun OrderCreateEditTotalsViewPreview() {
    OrderCreateEditTotalsView(
        state = TotalsSectionsState.Shown(
            button = TotalsSectionsState.Button(
                text = "Collect Payment",
                onClick = {}
            )
        )
    )
}
