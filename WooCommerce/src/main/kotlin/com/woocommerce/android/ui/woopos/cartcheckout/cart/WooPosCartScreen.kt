package com.woocommerce.android.ui.woopos.cartcheckout.cart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.cartcheckout.WooPosCartCheckoutUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosCartScreen(onCartCheckoutUIEvent: (WooPosCartCheckoutUIEvent) -> Unit) {
    val viewModel: WooPosCartViewModel = hiltViewModel()
    viewModel.state.observeAsState().value?.let { state ->
        WooPosCartScreen(
            state = state,
            { onCartCheckoutUIEvent(it) },
            viewModel::onUIEvent
        )
    }
}

@Composable
private fun WooPosCartScreen(
    state: WooPosCartState,
    onCartCheckoutUIEvent: (WooPosCartCheckoutUIEvent) -> Unit,
    onUIEvent: (WooPosCartUIEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Text(
                    text = "Cart",
                    style = MaterialTheme.typography.h3,
                    color = MaterialTheme.colors.primary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (state) {
                    WooPosCartState.Cart -> {
                        Button(onClick = {
                            onCartCheckoutUIEvent(WooPosCartCheckoutUIEvent.CheckoutClicked)
                            onUIEvent(WooPosCartUIEvent.CheckoutClicked)
                        }) {
                            Text("To Checkout")
                        }
                    }

                    WooPosCartState.Checkout -> {
                        Button(onClick = {
                            onCartCheckoutUIEvent(WooPosCartCheckoutUIEvent.BackFromCheckoytToCartClicked)
                            onUIEvent(WooPosCartUIEvent.BackFromCheckoutToCartClicked)
                        }) {
                            Text("To Products")
                        }
                    }
                }
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosCartScreen {}
}
