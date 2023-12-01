package com.woocommerce.android.ui.orders.creation.giftcards

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R

@Composable
fun EditGiftCardScreen(viewModel: OrderCreateEditGiftCardViewModel) {
    val giftCardValue by viewModel.giftCard.observeAsState()
    EditGiftCardScreen(giftCardValue.orEmpty(), viewModel::onGiftCardChanged, {})
}

@Composable
fun EditGiftCardScreen(
    giftCardValue: String,
    onTextChanged: (String) -> Unit,
    onDoneClicked: () -> Unit
) {
    Column {
        TextField(
            value = giftCardValue,
            onValueChange = onTextChanged,
            placeholder = {
                Text(stringResource(id = R.string.order_creation_gift_card_text_field_hint))
            },
            colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.surface),
            modifier = Modifier.fillMaxSize()
        )

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
            onClick = onDoneClicked
        ) {
            Text(stringResource(id = R.string.apply))
        }
    }
}

@Composable
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun EditGiftCardScreenPreview() {
    EditGiftCardScreen("XPTO-1234-ABCD-XPTO", {}, {})
}


