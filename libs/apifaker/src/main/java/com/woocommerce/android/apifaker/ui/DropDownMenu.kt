package com.woocommerce.android.apifaker.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun <T> DropDownMenu(
    label: String,
    currentValue: T,
    values: List<T>,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    formatter: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextField(
            readOnly = true,
            label = { Text(label) },
            value = formatter(currentValue), onValueChange = {},
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            values.forEach { value ->
                DropdownMenuItem(onClick = {
                    expanded = false
                    onValueChange(value)
                }) {
                    Text(formatter(value))
                }
            }
        }
    }
}
