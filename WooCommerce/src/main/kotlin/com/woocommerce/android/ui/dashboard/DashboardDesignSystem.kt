package com.woocommerce.android.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooIconButton
import com.woocommerce.android.ui.compose.designsystem.icons.Ellipsis
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons

@Composable
internal fun DashboardCardSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(WooTheme.radius.extraLarge)

    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = WooTheme.colors.surface.default,
            contentColor = WooTheme.colors.surface.onDefault,
            shadowElevation = WooTheme.spacing.space0,
            tonalElevation = WooTheme.spacing.space0,
            content = { Column(content = content) },
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = WooTheme.colors.surface.default,
            contentColor = WooTheme.colors.surface.onDefault,
            shadowElevation = WooTheme.spacing.space0,
            tonalElevation = WooTheme.spacing.space0,
            content = { Column(content = content) },
        )
    }
}

@Composable
internal fun <T> DashboardOverflowMenu(
    items: List<T>,
    onSelected: (T) -> Unit,
    mapper: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        WooIconButton(
            imageVector = WooIcons.Regular.Ellipsis,
            contentDescription = stringResource(R.string.more_options),
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = WooTheme.colors.surface.default,
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mapper(item),
                            style = WooTheme.text.bodyLarge.regular,
                            color = WooTheme.colors.surface.onDefault,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(item)
                    },
                )
            }
        }
    }
}

@Composable
internal fun DashboardSkeleton(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier = modifier
            .width(width)
            .height(height)
            .background(
                brush = dashboardSkeletonBrush(),
                shape = RoundedCornerShape(WooTheme.radius.small),
            ),
    )
}

@Composable
internal fun DashboardSkeleton(modifier: Modifier) {
    Spacer(
        modifier = modifier.background(
            brush = dashboardSkeletonBrush(),
            shape = RoundedCornerShape(WooTheme.radius.small),
        ),
    )
}

@Composable
private fun dashboardSkeletonBrush(): Brush {
    val baseColor = WooTheme.colors.stateLayers.onSurface.opacity16
    val highlightColor = WooTheme.colors.stateLayers.onSurface.opacity24
    val transition = rememberInfiniteTransition(label = "dashboard-skeleton-transition")
    val translation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "dashboard-skeleton-shimmer",
    )

    return Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset(10f, 10f),
        end = Offset(translation, translation),
    )
}
