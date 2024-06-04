package com.woocommerce.android.ui.woopos.home.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun Cart(onButtonClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface, RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Cart",
            style = MaterialTheme.typography.h3,
            color = MaterialTheme.colors.primary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onButtonClicked,
        ) {
            Text(stringResource(id = R.string.woopos_checkout))
        }
    }
}
