package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun WooPosSuccessCheckmark(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onAnimationStageChanged: (WooPosSuccessCheckmarkAnimationStage) -> Unit = {}
) {
    val isInstrumentedTest = remember {
        try {
            Class.forName("androidx.test.espresso.Espresso")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    val initialStage = if (isInstrumentedTest) {
        WooPosSuccessCheckmarkAnimationStage.FINISHED
    } else {
        WooPosSuccessCheckmarkAnimationStage.INITIAL
    }
    val savedAnimationStage = rememberSaveable { mutableStateOf(initialStage) }
    val animationStateFlow = remember { MutableStateFlow(savedAnimationStage.value) }

    LaunchedEffect(Unit) {
        if (!isInstrumentedTest && animationStateFlow.value != WooPosSuccessCheckmarkAnimationStage.FINISHED) {
            startSuccessCheckmarkAnimations(animationStateFlow)
        }
    }

    val animationStage = animationStateFlow.collectAsState().value
    savedAnimationStage.value = animationStage

    LaunchedEffect(animationStage) {
        onAnimationStageChanged(animationStage)
    }

    val size by animateDpAsState(
        targetValue = if (animationStage >= WooPosSuccessCheckmarkAnimationStage.CIRCLE) 166.dp else 0.dp,
        label = "Circle Size"
    )
    val iconSize by animateDpAsState(
        targetValue = if (animationStage >= WooPosSuccessCheckmarkAnimationStage.ICON) 72.dp else 0.dp,
        label = "Icon Size"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = WooPosElevation.Medium.value,
                shape = CircleShape,
                clip = false
            )
            .background(WooPosTheme.colors.success, CircleShape)
    ) {
        Icon(
            imageVector = WooPosIcons.Check,
            tint = WooPosTheme.colors.onSuccess,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Suppress("MagicNumber")
private suspend fun startSuccessCheckmarkAnimations(
    stateFlow: MutableStateFlow<WooPosSuccessCheckmarkAnimationStage>
) {
    stateFlow.update { WooPosSuccessCheckmarkAnimationStage.BUTTONS }
    delay(300)
    stateFlow.update { WooPosSuccessCheckmarkAnimationStage.CIRCLE }
    delay(300)
    stateFlow.update { WooPosSuccessCheckmarkAnimationStage.ICON }
    stateFlow.update { WooPosSuccessCheckmarkAnimationStage.FINISHED }
}

enum class WooPosSuccessCheckmarkAnimationStage {
    INITIAL,
    BUTTONS,
    CIRCLE,
    ICON,
    FINISHED,
}
