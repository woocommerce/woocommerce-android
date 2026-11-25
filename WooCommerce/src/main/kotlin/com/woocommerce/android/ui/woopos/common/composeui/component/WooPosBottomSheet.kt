package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WooPosBottomSheet(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSheetShown by remember { mutableStateOf(isVisible) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            isSheetShown = true
        } else if (isSheetShown) {
            sheetState.hide()
            isSheetShown = false
        }
    }

    if (isSheetShown) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = WooPosCornerRadius.Large.value,
                topEnd = WooPosCornerRadius.Large.value,
            ),
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            AnimatedVisibility(visible = true, enter = EnterTransition.None, exit = ExitTransition.None) {
                content()
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosBottomSheetPreview() {
    WooPosTheme {
        WooPosBottomSheet(
            isVisible = true,
            onDismissRequest = {},
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.XLarge.value),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WooPosText(
                    text = "Bottom sheet title",
                    style = WooPosTypography.Heading,
                    fontWeight = FontWeight.Bold,
                )
                WooPosText(
                    text = "As tall as its content.",
                    style = WooPosTypography.BodyLarge,
                )
            }
        }
    }
}
