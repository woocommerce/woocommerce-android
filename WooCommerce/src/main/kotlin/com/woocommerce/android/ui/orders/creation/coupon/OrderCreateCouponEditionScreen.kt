package com.woocommerce.android.ui.orders.creation.coupon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField

@Composable
fun OrderCreateCouponEditionScreen(
    state: State<OrderCreateCouponEditionViewModel.ViewState?>,
    onCouponCodeChanged: (String) -> Unit,
    onCouponRemoved: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth()
    ) {
        val focusRequester = remember { FocusRequester() }
        WCOutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = state.value?.couponCode ?: "",
            onValueChange = { onCouponCodeChanged(it) },
            label = stringResource(id = R.string.coupon_edit_code_hint)
        )
        if (state.value?.isRemoveButtonVisible == true) {
            WCColoredButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCouponRemoved() }
            ) {
                Text(stringResource(id = R.string.order_creation_remove_coupon))
            }
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }
}

@Preview
@Composable
fun OrderCreateCouponEditionScreenPreview() {
    OrderCreateCouponEditionScreen(
        state = object : State<OrderCreateCouponEditionViewModel.ViewState?> {
            override val value: OrderCreateCouponEditionViewModel.ViewState
                get() = OrderCreateCouponEditionViewModel.ViewState(true, "code", true)
        },
        onCouponCodeChanged = {},
        onCouponRemoved = {}
    )
}
