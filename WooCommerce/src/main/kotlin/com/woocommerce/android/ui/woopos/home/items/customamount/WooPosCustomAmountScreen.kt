package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosCustomAmountScreen(
    editing: WooPosCartItemViewState.CustomAmount?,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val close = { onNavigationEvent(WooPosNavigationEvent.CloseCustomAmount) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_custom_amount_form_title),
            onBackClicked = close,
        )
        WooPosCustomAmountFormScreen(
            editing = editing,
            onBackClick = close,
            onSubmitted = close,
            modifier = Modifier.weight(1f),
        )
    }
}
