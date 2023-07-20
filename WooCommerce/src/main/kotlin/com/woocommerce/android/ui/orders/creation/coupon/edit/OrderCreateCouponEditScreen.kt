package com.woocommerce.android.ui.orders.creation.coupon.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.orders.creation.coupon.edit.OrderCreateCouponEditViewModel.ValidationState.Idle

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
        val isError = state.value?.validationState is OrderCreateCouponEditViewModel.ValidationState.Error

        WCOutlinedTextField(
            modifier = Modifier.focusRequester(focusRequester),
            value = state.value?.couponCode ?: "",
            onValueChange = { onCouponCodeChanged(it) },
            label = stringResource(id = R.string.coupon_edit_code_hint),
            isError = isError,
            singleLine = true,
            trailingIcon = {
                if (isError) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = colorResource(id = R.color.woo_red_50)
                    )
                }
            },
        )
        if (isError) {
            val errorMessage: Int? = (
                state.value?.validationState as? OrderCreateCouponEditViewModel.ValidationState.Error
                )?.message
            Text(
                text = errorMessage?.let { stringResource(id = it) }.toString(),
                color = colorResource(id = R.color.woo_red_50),
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_100))
            )
        }
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
                get() = OrderCreateCouponEditViewModel.ViewState(
                    true, "code",
                    true, Idle
                )
        },
        onCouponCodeChanged = {},
        onCouponRemoved = {}
    )
}
