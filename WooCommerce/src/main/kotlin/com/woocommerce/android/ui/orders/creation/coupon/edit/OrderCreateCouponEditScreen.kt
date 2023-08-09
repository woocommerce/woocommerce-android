package com.woocommerce.android.ui.orders.creation.coupon.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton


@Composable
fun OrderCreateCouponEditScreen(
    state: State<OrderCreateCouponEditViewModel.ViewState?>,
    onCouponRemoved: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth()
    ) {
        Text(
            text = state.value?.couponCode ?: "",
            maxLines = 1,
        )

        if (state.value?.isRemoveButtonVisible == true) {
            WCColoredButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCouponRemoved() }
            ) {
                Text(stringResource(id = R.string.order_creation_remove_coupon))
            }
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
                    "code",
                    true,
                )
        },
        onCouponRemoved = {}
    )
}
