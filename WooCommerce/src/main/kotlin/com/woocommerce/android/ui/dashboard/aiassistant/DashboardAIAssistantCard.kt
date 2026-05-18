package com.woocommerce.android.ui.dashboard.aiassistant

import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.aiassistant.R as AiAssistantR

@Composable
fun DashboardAIAssistantCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.dashboard_ai_assistant_entry_point_content_description)
    val cardShape = RoundedCornerShape(20.dp)
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            },
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = dimensionResource(R.dimen.minor_10),
            color = colorResource(R.color.woo_gray_5),
        )
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = dimensionResource(R.dimen.major_100),
                    vertical = dimensionResource(R.dimen.major_75),
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.major_75)),
        ) {
            SparkleChip()
            Text(
                text = stringResource(R.string.dashboard_ai_assistant_entry_point_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
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
    val chipBackground = if (isSystemInDarkTheme()) {
        colorResource(R.color.woo_purple_60).copy(alpha = CHIP_BACKGROUND_ALPHA_DARK)
    } else {
        colorResource(R.color.woo_purple_10)
    }
    val sparkleTint = if (isSystemInDarkTheme()) {
        colorResource(R.color.woo_purple_30)
    } else {
        colorResource(R.color.woo_purple_40)
    }

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
            .size(40.dp)
            .clip(RoundedCornerShape(dimensionResource(R.dimen.minor_100)))
            .background(chipBackground),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(AiAssistantR.drawable.ic_assistant_sparkle),
            contentDescription = null,
            tint = sparkleTint,
            modifier = Modifier
                .size(20.dp)
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
            .size(32.dp)
            .clip(CircleShape)
            .background(colorResource(R.color.woo_purple_40)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_gridicons_arrow_up),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

private const val SPARKLE_SCALE_MIN = 0.95f
private const val SPARKLE_SCALE_MAX = 1.0f
private const val SPARKLE_ALPHA_MIN = 0.6f
private const val SPARKLE_ALPHA_MAX = 1.0f
private const val SPARKLE_PULSE_DURATION_MS = 800
private const val CHIP_BACKGROUND_ALPHA_DARK = 0.20f

@LightDarkThemePreviews
@Composable
private fun DashboardAIAssistantCardPreview() {
    WooThemeWithBackground {
        DashboardAIAssistantCard(onClick = {})
    }
}
