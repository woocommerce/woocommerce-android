package com.woocommerce.android.ui.orders.creation.totals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun OrderCreateEditTotalsView(
    state: TotalsSectionsState,
    isPreview: Boolean = LocalInspectionMode.current,
) {
    WooThemeWithBackground {
        val visible = state is TotalsSectionsState.Shown
        AnimatedVisibility(
            visible = visible || isPreview,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Card(
                shape = RoundedCornerShape(0.dp),
                elevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Divider(modifier = Modifier.fillMaxWidth())

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = colorResource(id = R.color.color_primary)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    WCColoredButton(
                        onClick = {
                            (state as? TotalsSectionsState.Shown)?.button?.onClick?.invoke()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
                    ) {
                        Text(
                            text = (state as? TotalsSectionsState.Shown)?.button?.text ?: "",
                        )
                    }
                }
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
