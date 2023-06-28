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
import com.woocommerce.android.ui.orders.creation.coupon.OrderCreateCouponEditViewModel.ValidationState.ERROR
import com.woocommerce.android.ui.orders.creation.coupon.OrderCreateCouponEditViewModel.ValidationState.IDLE

@Composable
fun OrderCreateCouponEditScreen(
    state: State<OrderCreateCouponEditViewModel.ViewState?>,
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
        val isError = state.value?.validationState == ERROR
        WCOutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = state.value?.couponCode ?: "",
            onValueChange = { onCouponCodeChanged(it) },
            label = stringResource(id = R.string.coupon_edit_code_hint),
            isError = isError,
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
    OrderCreateCouponEditScreen(
        state = object : State<OrderCreateCouponEditViewModel.ViewState?> {
            override val value: OrderCreateCouponEditViewModel.ViewState
                get() = OrderCreateCouponEditViewModel.ViewState(true, "code", true, IDLE)
        },
        onCouponCodeChanged = {},
        onCouponRemoved = {}
    )
}
