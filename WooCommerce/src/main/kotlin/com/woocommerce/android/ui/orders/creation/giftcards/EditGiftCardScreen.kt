package com.woocommerce.android.ui.orders.creation.giftcards

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R

@Composable
fun EditGiftCardScreen(viewModel: OrderCreateEditGiftCardViewModel) {
    val giftCardValue by viewModel.giftCard.observeAsState()
    EditGiftCardScreen(giftCardValue.orEmpty(), viewModel::onGiftCardChanged)
}

@Composable
fun EditGiftCardScreen(giftCardValue: String, onTextChanged: (String) -> Unit) {
    TextField(
        value = giftCardValue,
        onValueChange = onTextChanged,
        placeholder = {
            Text(stringResource(id = R.string.order_creation_gift_card_text_field_hint))
        },
        colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.surface),
        modifier = Modifier.fillMaxSize()
    )
}
