package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        Box {
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
                modifier = modifier
                    .focusable(false)
            )
            // Capture and consume click events, this makes the text field non-focusable too.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = enabled, onClick = onClick)
            )
        }
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

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SpinnerPreview() {
    WooThemeWithBackground {
        var text by remember {
            mutableStateOf("button")
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            WCOutlinedSpinner(onClick = { /*TODO*/ }, value = "", label = "Label")
            WCOutlinedSpinner(onClick = { /*TODO*/ }, value = "Value", label = "Label")
            WCOutlinedSpinner(
                onClick = { /*TODO*/ },
                value = "Value",
                label = "Label",
                helperText = "Helper Text"
            )
            WCOutlinedSpinner(onClick = { text = "clicked" }, value = text, label = "Label", enabled = true)
        }
    }
}
