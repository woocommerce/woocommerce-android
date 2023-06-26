package com.woocommerce.android.ui.products

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton

@Composable
fun ProductDescriptionAICelebrationBottomSheet(viewModel: ProductDescriptionAICelebrationViewModel) {
    Text(text = viewModel.toString())

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.product_description_ai_note_dialog_heading),
            style = MaterialTheme.typography.h4,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        Text(
            text = stringResource(id = R.string.product_description_ai_note_dialog_message),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))

        WCColoredButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = R.string.product_description_ai_note_dialog_confirmation))
        }
    }
}
