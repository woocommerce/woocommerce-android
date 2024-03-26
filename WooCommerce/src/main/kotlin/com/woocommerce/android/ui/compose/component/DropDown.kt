package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> WcExposedDropDown(
    items: Array<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    currentValue: T = items.first(),
    mapper: (T) -> String = { it.toString() },
    focusRequester: FocusRequester = FocusRequester()
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(currentValue) }

    LaunchedEffect(currentValue) {
        selectedText = currentValue
    }

    Box(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {
            OutlinedTextField(
                value = mapper(selectedText),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.focusRequester(focusRequester)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        content = { Text(text = mapper(item)) },
                        onClick = {
                            selectedText = item
                            expanded = false
                            onSelected(selectedText)
                        }
                    )
                }
            }
        }
    }
}
