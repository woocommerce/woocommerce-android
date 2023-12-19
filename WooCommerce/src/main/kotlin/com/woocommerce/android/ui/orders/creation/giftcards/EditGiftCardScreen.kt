package com.woocommerce.android.ui.orders.creation.giftcards

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField

@Composable
fun EditGiftCardScreen(viewModel: OrderCreateEditGiftCardViewModel) {
    val giftCardValue by viewModel.giftCard.observeAsState()
    EditGiftCardScreen(
        giftCardValue = giftCardValue.orEmpty(),
        onTextChanged = viewModel::onGiftCardChanged,
        onDoneClicked = viewModel::onDoneButtonClicked
    )
}

@Composable
fun EditGiftCardScreen(
    giftCardValue: String,
    onTextChanged: (String) -> Unit,
    onDoneClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        WCOutlinedTextField(
            value = giftCardValue,
            onValueChange = onTextChanged,
            label = stringResource(id = R.string.order_creation_gift_card_text_field_hint),
            colors = TextFieldDefaults.textFieldColors(backgroundColor = MaterialTheme.colors.surface),
            modifier = modifier.fillMaxWidth()
        )

        Button(
            modifier = modifier
                .fillMaxWidth(),
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
