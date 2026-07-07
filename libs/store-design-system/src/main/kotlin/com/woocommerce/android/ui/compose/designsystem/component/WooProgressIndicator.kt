package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
fun WooLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    if (progress == null) {
        LinearProgressIndicator(
            modifier = modifier,
        )
    } else {
        val coercedProgress = progress.coerceIn(MIN_PROGRESS, MAX_PROGRESS)

        LinearProgressIndicator(
            progress = { coercedProgress },
            modifier = modifier,
        )
    }
}

@Composable
fun WooCircularProgressIndicator(
    modifier: Modifier = Modifier,
    progress: Float? = null,
) {
    if (progress == null) {
        CircularProgressIndicator(
            modifier = modifier,
        )
    } else {
        val coercedProgress = progress.coerceIn(MIN_PROGRESS, MAX_PROGRESS)

        CircularProgressIndicator(
            progress = { coercedProgress },
            modifier = modifier,
        )
    }
}

@Suppress("UnusedPrivateMember")
@PreviewLightDark
@Composable
private fun WooProgressIndicatorPreview() {
    WooDesignSystemTheme {
        Surface(color = WooTheme.colors.background.section) {
            WooProgressIndicatorDemo(modifier = Modifier.padding(WooTheme.padding.padding5))
        }
    }
}

@Composable
internal fun WooProgressIndicatorDemo(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4),
    ) {
        WooLinearProgressIndicator(
            progress = 0.64f,
            modifier = Modifier.width(180.dp),
        )
        WooLinearProgressIndicator(modifier = Modifier.width(180.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space4)) {
            WooCircularProgressIndicator(
                progress = 0.64f,
                modifier = Modifier.size(36.dp),
            )
            WooCircularProgressIndicator(modifier = Modifier.size(36.dp))
        }
    }
}

private const val MIN_PROGRESS = 0f
private const val MAX_PROGRESS = 1f
