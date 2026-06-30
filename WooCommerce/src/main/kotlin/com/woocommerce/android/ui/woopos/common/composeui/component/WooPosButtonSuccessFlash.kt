package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

private const val SUCCESS_FLASH_DURATION_MS = 1000L
private val SUCCESS_CHECK_SIZE = 40.dp

@Composable
fun rememberSuccessFlash(
    trigger: Flow<Unit>,
    durationMillis: Long = SUCCESS_FLASH_DURATION_MS,
): Boolean {
    var flashing by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        trigger.collectLatest {
            flashing = true
            delay(durationMillis)
            flashing = false
        }
    }
    return flashing
}

@Composable
fun BoxScope.WooPosButtonSuccessFlash(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(300)),
        modifier = modifier.matchParentSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
                .background(WooPosTheme.colors.success),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = WooPosIcons.Check,
                contentDescription = null,
                tint = WooPosTheme.colors.onSuccess,
                modifier = Modifier.size(SUCCESS_CHECK_SIZE),
            )
        }
    }
}
