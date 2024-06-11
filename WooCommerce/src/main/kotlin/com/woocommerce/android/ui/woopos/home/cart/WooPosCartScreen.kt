package com.woocommerce.android.ui.woopos.home.cart

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosCartScreen() {
    val viewModel: WooPosCartViewModel = hiltViewModel()
    WooPosCartScreen(
        state = viewModel.state.collectAsState().value,
        viewModel::onUIEvent
    )
}

@Composable
private fun WooPosCartScreen(
    state: WooPosCartState,
    onUIEvent: (WooPosCartUIEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.woo_pos_car_pane_title),
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.primary,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(state.itemsInCart) { item ->
                    ProductItem(
                        item,
                        state.areItemsRemovable
                    ) { onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(item)) }
                }
            }

            when (state) {
                is WooPosCartState.Cart -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.itemsInCart.isNotEmpty() && !state.isOrderCreationInProgress,
                        onClick = { onUIEvent(WooPosCartUIEvent.CheckoutClicked) }
                    ) {
                        Text(stringResource(R.string.woo_pos_checkout_button))
                    }
                }

                is WooPosCartState.Checkout -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isOrderCreationInProgress,
                        onClick = {
                            onUIEvent(WooPosCartUIEvent.BackFromCheckoutToCartClicked)
                        }
                    ) {
                        Text("To Products")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductItem(
    product: WooPosCartListItem,
    canRemoveItems: Boolean,
    onRemoveClicked: (item: WooPosCartListItem) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .border(1.dp, Color.Gray, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
    ) {
        val (title, removeButton) = createRefs()
        Text(
            modifier = Modifier.constrainAs(title) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(removeButton.start)
                width = Dimension.fillToConstraints
            },
            text = product.title,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (canRemoveItems) {
            IconButton(
                modifier = Modifier.constrainAs(removeButton) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
                onClick = { onRemoveClicked(product) }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.woo_pos_remove_item_from_cart_content_description)
                )
            }
        }
    }
}

@Composable
@WooPosPreview
fun ProductItemPreview() {
    val item = WooPosCartListItem(1L, "VW California")
    ProductItem(item, true) {}
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosCartScreen()
}
