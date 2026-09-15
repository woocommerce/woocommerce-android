package com.woocommerce.android.ui.dashboard.aiassistant

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.dashboard.DashboardCardSurface
import com.woocommerce.android.aiassistant.R as AiAssistantR

@Composable
fun DashboardAIAssistantCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.dashboard_ai_assistant_entry_point_content_description)
    DashboardCardSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            },
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = WooTheme.padding.padding5,
                    vertical = WooTheme.padding.padding4,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
        ) {
            SparkleChip()
            Text(
                text = stringResource(R.string.dashboard_ai_assistant_entry_point_label),
                color = WooTheme.colors.surface.onDefault,
                style = WooTheme.text.bodyLarge.regular,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            SendAffordance()
        }
    }
}

@Composable
private fun SparkleChip() {
    val chipBackground = WooTheme.colors.container.secondaryContainer
    val sparkleTint = WooTheme.colors.container.onSecondaryContainer

    val transition = rememberInfiniteTransition(label = "ai-sparkle-pulse")
    val scale by transition.animateFloat(
        initialValue = SPARKLE_SCALE_MIN,
        targetValue = SPARKLE_SCALE_MAX,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SPARKLE_PULSE_DURATION_MS, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ai-sparkle-scale",
    )
    val alpha by transition.animateFloat(
        initialValue = SPARKLE_ALPHA_MIN,
        targetValue = SPARKLE_ALPHA_MAX,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SPARKLE_PULSE_DURATION_MS, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ai-sparkle-alpha",
    )

    Box(
        modifier = Modifier
            .size(WooTheme.spacing.space9)
            .clip(RoundedCornerShape(WooTheme.radius.medium))
            .background(chipBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(AiAssistantR.drawable.ic_assistant_sparkle),
            contentDescription = null,
            tint = sparkleTint,
            modifier = Modifier
                .size(WooTheme.iconSize.size20)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
        )
    }
}

@Composable
private fun SendAffordance() {
    Box(
        modifier = Modifier
            .size(WooTheme.spacing.space8)
            .clip(CircleShape)
            .background(WooTheme.colors.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_gridicons_arrow_up),
            contentDescription = null,
            tint = WooTheme.colors.onPrimary,
            modifier = Modifier.size(WooTheme.iconSize.size18),
        )
    }
}

private const val SPARKLE_SCALE_MIN = 0.95f
private const val SPARKLE_SCALE_MAX = 1.0f
private const val SPARKLE_ALPHA_MIN = 0.6f
private const val SPARKLE_ALPHA_MAX = 1.0f
private const val SPARKLE_PULSE_DURATION_MS = 800

@PreviewLightDark
@Composable
private fun DashboardAIAssistantCardPreview() {
    WooDesignSystemThemeWithBackground {
        DashboardAIAssistantCard(onClick = {})
    }
}
