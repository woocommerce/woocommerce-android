package com.woocommerce.android.ui.orders.creation.totals

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooTheme
import kotlinx.coroutines.launch

@Composable
fun OrderCreateEditTotalsView(
    state: TotalsSectionsState,
    isPreview: Boolean = LocalInspectionMode.current,
) {
    WooTheme {
        val visible = state is TotalsSectionsState.Shown
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            visible = visible || isPreview,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .background(color = Color.Transparent),
            ) {
                val shadowHeight = dimensionResource(id = R.dimen.minor_100)
                val shadowHeightPx = with(LocalDensity.current) { shadowHeight.toPx() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.15f),
                                ),
                                startY = 0f,
                                endY = shadowHeightPx,
                            )
                        )
                        .height(shadowHeight)
                )

                TotalsView(state)
            }
        }
    }
}

@Composable
private fun TotalsView(state: TotalsSectionsState) {
    var isExpanded by remember { mutableStateOf(false) }
    val totalsIs = remember { MutableInteractionSource() }
    val topRowCoroutineScope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = colorResource(id = R.color.color_surface)
            )
            .clickable(
                interactionSource = totalsIs,
                indication = null
            ) {
                val press = PressInteraction.Press(Offset.Zero)
                topRowCoroutineScope.launch {
                    totalsIs.emit(press)
                    totalsIs.emit(PressInteraction.Release(press))
                }
                isExpanded = !isExpanded
            }
    ) {
        Crossfade(
            targetState = isExpanded,
            label = "totals_icon"
        ) { expanded ->
            IconButton(interactionSource = totalsIs, onClick = {}) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = colorResource(id = R.color.color_primary),
                    modifier = Modifier.padding(all = dimensionResource(id = R.dimen.minor_100))
                )
            }
        }

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

@Composable
@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
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
