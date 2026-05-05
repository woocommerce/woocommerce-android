package com.woocommerce.android.ui.woopos.home.totals

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosBackButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint

@Composable
internal fun WooPosAllPaymentMethodsDialog(
    isVisible: Boolean,
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currentWooPosBreakpoint() == WooPosBreakpoint.Phone) {
        PaymentMethodsFullscreen(
            isVisible = isVisible,
            methods = methods,
            onMethodClicked = onMethodClicked,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
        )
    } else {
        PaymentMethodsDialog(
            isVisible = isVisible,
            methods = methods,
            onMethodClicked = onMethodClicked,
            onDismissRequest = onDismissRequest,
            modifier = modifier,
        )
    }
}

@Composable
private fun PaymentMethodsDialog(
    isVisible: Boolean,
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosDialogWrapper(
        modifier = modifier,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_payment_method_picker_dialog_title
        ),
        onCloseClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_payment_method_picker_dialog_title),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
            PaymentMethodButtons(methods = methods, onMethodClicked = onMethodClicked)
        }
    }
}

@Composable
private fun PaymentMethodsFullscreen(
    isVisible: Boolean,
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = isVisible) { onDismissRequest() }
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WooPosBackButton(
                        modifier = Modifier.padding(start = WooPosSpacing.Small.value),
                        onClick = onDismissRequest,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = WooPosSpacing.Large.value),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    WooPosText(
                        text = stringResource(R.string.woopos_payment_method_picker_dialog_title),
                        style = WooPosTypography.Heading,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))
                    PaymentMethodButtons(methods = methods, onMethodClicked = onMethodClicked)
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodButtons(
    methods: List<WooPosPaymentMethod>,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
) {
    methods.forEach { method ->
        WooPosOutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(method.testTag()),
            text = stringResource(method.labelRes()),
            onClick = { onMethodClicked(method) },
        )
        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))
    }
}
