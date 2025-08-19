package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Announces the given text to accessibility services using a live region.
 * Ensures screen readers pick up the message even on repeated values.
 */
@Composable
fun AccessibilityAnnouncement(text: String) {
    key(text + System.currentTimeMillis()) { // Forces recomposition
        // Intentionally using Text instead of WooPosText to ensure accessibility announcements work reliably.
        // WooPosText might change or wrap semantics in a way that breaks live region behavior.
        @Suppress("WooPosDesignSystemTextUsageRule")
        Text(
            text = text,
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .semantics {
                    liveRegion = LiveRegionMode.Assertive
                }
        )
    }
}
