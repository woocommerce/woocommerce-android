package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun WCOutlinedSpinner(
    onClick: () -> Unit,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = {
                Text(text = label)
            },
            readOnly = true,
            trailingIcon = {
                Icon(painter = painterResource(id = R.drawable.ic_arrow_drop_down), contentDescription = null)
            },
            enabled = enabled,
            modifier = modifier.clickable(onClick = onClick, enabled = enabled)
        )
        if (!helperText.isNullOrEmpty()) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.caption,
                color = colorResource(id = color.color_on_surface_medium),
                modifier = Modifier.padding(horizontal = dimensionResource(id = dimen.major_100))
            )
        }
    }
}

@Preview
@Composable
fun SpinnerPreview() {
    WooThemeWithBackground {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WCOutlinedSpinner(onClick = { /*TODO*/ }, value = "", label = "Label")
            WCOutlinedSpinner(onClick = { /*TODO*/ }, value = "Value", label = "Label")
            WCOutlinedSpinner(onClick = { /*TODO*/ }, value = "Value", label = "Label", helperText = "Helper Text")
            WCOutlinedSpinner(onClick = { /*TODO*/ }, value = "Value", label = "Label", enabled = false)
        }
    }
}
