package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.currentWooPosBreakpoint
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

@Composable
internal fun WooPosCheckoutPaymentButtons(
    readerStatus: WooPosTotalsViewState.ReaderStatus,
    isTapToPayAvailable: Boolean,
    onMethodClicked: (WooPosPaymentMethod) -> Unit,
    onShowAllMethods: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPhone = currentWooPosBreakpoint() == WooPosBreakpoint.Phone
    val outerPaddingModifier = if (isPhone) {
        Modifier.padding(WooPosSpacing.Large.value)
    } else {
        Modifier.padding(horizontal = WooPosSpacing.XLarge.value)
    }
    val layout = derivePaymentButtonsLayout(
        formFactor = if (isPhone) WooPosFormFactor.PHONE else WooPosFormFactor.TABLET,
        readerStatus = readerStatus,
        isTapToPayAvailable = isTapToPayAvailable,
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(outerPaddingModifier)
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        when (layout) {
            is WooPosPaymentButtonsLayout.Single -> {
                MethodButton(method = layout.primary, onClick = { onMethodClicked(layout.primary) })
            }
            is WooPosPaymentButtonsLayout.Pair -> {
                MethodButton(method = layout.primary, onClick = { onMethodClicked(layout.primary) })
                MethodButton(method = layout.secondary, onClick = { onMethodClicked(layout.secondary) })
            }
            is WooPosPaymentButtonsLayout.WithOverflow -> {
                MethodButton(method = layout.primary, onClick = { onMethodClicked(layout.primary) })
                AllPaymentMethodsButton(onClick = onShowAllMethods)
            }
        }
    }
}

@Composable
private fun RowScope.MethodButton(
    method: WooPosPaymentMethod,
    onClick: () -> Unit,
) {
    WooPosOutlinedButton(
        modifier = Modifier
            .weight(1f)
            .testTag(method.testTag()),
        text = stringResource(method.labelRes()),
        onClick = onClick,
    )
}

@Composable
private fun RowScope.AllPaymentMethodsButton(onClick: () -> Unit) {
    WooPosOutlinedButton(
        modifier = Modifier
            .weight(1f)
            .testTag(WooPosTestTags.ALL_PAYMENT_METHODS_BUTTON),
        text = stringResource(R.string.woopos_payment_method_all_methods_label),
        onClick = onClick,
    )
}

internal sealed interface WooPosPaymentButtonsLayout {
    data class Single(val primary: WooPosPaymentMethod) : WooPosPaymentButtonsLayout
    data class Pair(val primary: WooPosPaymentMethod, val secondary: WooPosPaymentMethod) : WooPosPaymentButtonsLayout
    data class WithOverflow(val primary: WooPosPaymentMethod) : WooPosPaymentButtonsLayout
}

internal enum class WooPosFormFactor { PHONE, TABLET }

private const val OVERFLOW_THRESHOLD = 3

internal fun derivePaymentButtonsLayout(
    formFactor: WooPosFormFactor,
    readerStatus: WooPosTotalsViewState.ReaderStatus,
    isTapToPayAvailable: Boolean,
): WooPosPaymentButtonsLayout {
    val methods = availableMethods(readerStatus, isTapToPayAvailable)
    if (methods.size == 1) return WooPosPaymentButtonsLayout.Single(methods.single())
    val primary = pickPrimary(formFactor, methods)
    if (methods.size >= OVERFLOW_THRESHOLD) return WooPosPaymentButtonsLayout.WithOverflow(primary)
    val secondary = methods.first { it != primary }
    return WooPosPaymentButtonsLayout.Pair(primary, secondary)
}

private fun availableMethods(
    readerStatus: WooPosTotalsViewState.ReaderStatus,
    isTapToPayAvailable: Boolean,
): List<WooPosPaymentMethod> {
    val isReaderDisconnected = readerStatus is WooPosTotalsViewState.ReaderStatus.Disconnected
    return buildList {
        if (isReaderDisconnected) add(WooPosPaymentMethod.CARD_READER)
        if (isTapToPayAvailable) add(WooPosPaymentMethod.TAP_TO_PAY)
        add(WooPosPaymentMethod.CASH)
    }
}

private fun pickPrimary(
    formFactor: WooPosFormFactor,
    methods: List<WooPosPaymentMethod>,
): WooPosPaymentMethod {
    val preference = when (formFactor) {
        WooPosFormFactor.PHONE -> listOf(
            WooPosPaymentMethod.TAP_TO_PAY,
            WooPosPaymentMethod.CARD_READER,
            WooPosPaymentMethod.CASH,
        )
        WooPosFormFactor.TABLET -> listOf(
            WooPosPaymentMethod.CARD_READER,
            WooPosPaymentMethod.CASH,
            WooPosPaymentMethod.TAP_TO_PAY,
        )
    }
    return preference.first { it in methods }
}
