package com.woocommerce.android.ui.woopos.home.totals

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

private const val ANIMATION_DURATION_MS = 300
private const val SCRIM_ALPHA = 0.4f

@Composable
internal fun WooPosAllPaymentMethodsBottomSheet(
    isVisible: Boolean,
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = isVisible) { onDismissRequest() }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(animationSpec = tween(ANIMATION_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(ANIMATION_DURATION_MS)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )
        }
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                animationSpec = tween(ANIMATION_DURATION_MS),
                initialOffsetY = { it },
            ),
            exit = slideOutVertically(
                animationSpec = tween(ANIMATION_DURATION_MS),
                targetOffsetY = { it },
            ),
        ) {
            Surface(
                modifier = Modifier
                    .padding(horizontal = WooPosSpacing.Small.value)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = WooPosCornerRadius.Large.value,
                    topEnd = WooPosCornerRadius.Large.value,
                ),
                color = MaterialTheme.colorScheme.surfaceBright,
                shadowElevation = WooPosElevation.Medium.value,
            ) {
                PaymentMethodsBottomSheetContent(
                    methods = methods,
                    onMethodClicked = onMethodClicked,
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodsBottomSheetContent(
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = WooPosSpacing.XLarge.value,
                vertical = WooPosSpacing.XLarge.value,
            )
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_payment_method_picker_bottom_sheet_title),
            style = WooPosTypography.Heading,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
        PaymentMethodButtons(methods = methods, onMethodClicked = onMethodClicked)
    }
}

@Composable
private fun PaymentMethodButtons(
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        methods.forEach { method ->
            WooPosOutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(method.testTag()),
                text = stringResource(method.labelRes()),
                onClick = { onMethodClicked(method) },
            )
        }
    }
}

@Composable
@WooPosPreview
fun PaymentMethodsBottomSheetReaderConnectedPreview() {
    WooPosTheme {
        WooPosAllPaymentMethodsBottomSheet(
            isVisible = true,
            methods = listOf(
                WooPosPaymentMethod.TAP_TO_PAY,
                WooPosPaymentMethod.SCAN_TO_PAY,
                WooPosPaymentMethod.MARK_ORDER_AS_PAID,
            ),
            onMethodClicked = {},
            onDismissRequest = {},
        )
    }
}

@Composable
@WooPosPreview
fun PaymentMethodsBottomSheetReaderDisconnectedPreview() {
    WooPosTheme {
        WooPosAllPaymentMethodsBottomSheet(
            isVisible = true,
            methods = listOf(
                WooPosPaymentMethod.CARD_READER,
                WooPosPaymentMethod.SCAN_TO_PAY,
                WooPosPaymentMethod.MARK_ORDER_AS_PAID,
            ),
            onMethodClicked = {},
            onDismissRequest = {},
        )
    }
}
