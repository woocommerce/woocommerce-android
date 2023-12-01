package com.woocommerce.android.ui.orders.creation.giftcards

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R

@Composable
fun EditGiftCardScreen(viewModel: OrderCreateEditGiftCardViewModel) {
    EditGiftCardScreen(viewModel::onGiftCardChanged)
}

@Composable
fun EditGiftCardScreen(onTextChanged: (String) -> Unit) {
    TextField(
        value = stringResource(id = R.string.gift_card_default_value),
        onValueChange = onTextChanged,
        placeholder = {
            Text(stringResource(id = R.string.gift_card_hint_value))
        },
        colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.surface),
        modifier = Modifier.fillMaxSize()
    )
}
