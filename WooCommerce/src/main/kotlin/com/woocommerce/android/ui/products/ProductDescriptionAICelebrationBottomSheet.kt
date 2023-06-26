package com.woocommerce.android.ui.products

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton

@Composable
fun ProductDescriptionAICelebrationBottomSheet(viewModel: ProductDescriptionAICelebrationViewModel) {
    ProductDescriptionAICelebrationBottomSheet(
        viewModel::onConfirmedClick
    )
}
@Composable
fun ProductDescriptionAICelebrationBottomSheet(
    onConfirmClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .padding(all = dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_ai_generated_content),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.product_description_ai_note_dialog_heading),
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.product_description_ai_note_dialog_message),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        WCColoredButton(onClick = onConfirmClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.product_description_ai_note_dialog_confirmation))
        }
    }
}

@Preview
@Composable
fun ProductDescriptionAICelebrationBottomSheetPreview() {
    ProductDescriptionAICelebrationBottomSheet(
        onConfirmClick = { }
    )
}
