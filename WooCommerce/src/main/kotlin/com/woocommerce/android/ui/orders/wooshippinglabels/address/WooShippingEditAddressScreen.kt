package com.woocommerce.android.ui.orders.wooshippinglabels.address

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.orders.wooshippinglabels.RoundedCornerBoxWithBorder

@Composable
fun WooShippingEditAddressScreen(
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.woo_shipping_edit_origin_address_title),
                onNavigationButtonClick = {},
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack
            )
        },
        backgroundColor = MaterialTheme.colors.surface
    ) { padding ->
        Column(
            modifier
                .verticalScroll(rememberScrollState())
                .padding(padding)
        ) {
            RoundedBorderTextFieldWithLabel(
                label = "${stringResource(id = R.string.woo_shipping_label_name)} *",
                text = "",
                onTextChange = {},
            )

            var isExpanded by remember { mutableStateOf(false) }

            CollapsedField(
                isExpanded = isExpanded,
                label = stringResource(id = R.string.woo_shipping_label_company),
                onExpand = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                RoundedBorderTextFieldWithLabel(
                    label = stringResource(id = R.string.woo_shipping_label_company),
                    text = "",
                    onTextChange = {},
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            RoundedBorderDropDownWithLabel(
                label = "${stringResource(id = R.string.woo_shipping_label_country)} *",
                text = "United States",
                modifier = Modifier.padding(top = 24.dp),
                onClick = {}
            )
            RoundedBorderTextFieldWithLabel(
                label = "${stringResource(id = R.string.woo_shipping_label_address)} *",
                text = "",
                onTextChange = {},
                modifier = Modifier.padding(top = 8.dp)
            )
            RoundedBorderTextFieldWithLabel(
                label = "${stringResource(id = R.string.woo_shipping_label_city)} *",
                text = "",
                onTextChange = {},
                modifier = Modifier.padding(top = 8.dp)
            )

            Row {
                RoundedBorderDropDownWithLabel(
                    label = "${stringResource(id = R.string.woo_shipping_label_state)} *",
                    text = "United States",
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .weight(1f),
                    onClick = {}
                )
                Spacer(modifier = Modifier.size(8.dp))
                RoundedBorderTextFieldWithLabel(
                    label = "${stringResource(id = R.string.woo_shipping_label_post_code)} *",
                    text = "",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    onTextChange = {},
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .weight(1f)
                )
            }

            RoundedBorderTextFieldWithLabel(
                label = "${stringResource(id = R.string.woo_shipping_label_email)} *",
                text = "",
                onTextChange = {},
                modifier = Modifier.padding(top = 32.dp)
            )
            RoundedBorderTextFieldWithLabel(
                label = "${stringResource(id = R.string.woo_shipping_label_phone)} *",
                text = "",
                onTextChange = {},
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
internal fun RoundedBorderTextFieldWithLabel(
    label: String,
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "",
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    endSection: @Composable () -> Unit = {}
) {
    val hasError = error.isNotNullOrEmpty()
    val color = if (hasError) {
        MaterialTheme.colors.error
    } else {
        colorResource(R.color.divider_color)
    }
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        RoundedCornerBoxWithBorder(
            borderColor = color
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = text,
                        onValueChange = onTextChange,
                        keyboardOptions = keyboardOptions,
                        textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onSurface),

                    )
                    if (text.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.body2,
                            color = colorResource(id = R.color.color_on_surface_disabled)
                        )
                    }
                }
                endSection()
            }
        }
        error?.let {
            Text(
                text = error,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Preview
@Composable
private fun RoundedBorderTextFieldWithLabelPreview() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .padding(16.dp)
    ) {
        RoundedBorderTextFieldWithLabel(
            label = "Label",
            text = "123",
            onTextChange = {},
            hint = "Hint"
        )
        Spacer(modifier = Modifier.padding(8.dp))
        RoundedBorderTextFieldWithLabel(
            label = "Empty text field",
            text = "",
            onTextChange = {},
            hint = "Hint"
        )
        Spacer(modifier = Modifier.padding(8.dp))
        RoundedBorderTextFieldWithLabel(
            label = "Required field",
            text = "",
            onTextChange = {},
            hint = "Hint",
            error = "This field is required"
        )
    }
}

@Composable
private fun RoundedBorderDropDownWithLabel(
    label: String,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        RoundedCornerBoxWithBorder(innerModifier = Modifier.clickable { onClick() }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface
                )
            }
        }
    }
}

@Preview
@Composable
private fun RoundedBorderDropDownWithLabelPreview() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .padding(16.dp)
    ) {
        RoundedBorderDropDownWithLabel(
            label = "Label",
            text = "Text",
            onClick = {}
        )
    }
}

@Composable
private fun CollapsedField(
    label: String,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedContent(targetState = isExpanded, label = "expand_field_animation") { value ->
        if (value) {
            content()
        } else {
            Row(
                modifier = modifier
                    .clickable { onExpand() }
                    .padding(vertical = 16.dp, horizontal = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary
                )
                Text(
                    text = stringResource(id = R.string.add, label.lowercase()),
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
