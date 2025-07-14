package com.woocommerce.android.ui.orders.creation.coupon.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCRemoveButton

@Composable
fun OrderCreateCouponEditScreen(
    state: State<OrderCreateCouponDetailsViewModel.ViewState?>,
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
            WCRemoveButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onCouponRemoved() },
                text = stringResource(id = R.string.order_creation_remove_coupon)
            )
        }
    }
}

@Preview
@Composable
fun OrderCreateCouponEditionScreenPreview() {
    OrderCreateCouponEditScreen(
        state = remember {
            mutableStateOf(
                OrderCreateCouponDetailsViewModel.ViewState(
                    "code",
                    true,
                )
            )
        },
        onCouponRemoved = {}
    )
}
