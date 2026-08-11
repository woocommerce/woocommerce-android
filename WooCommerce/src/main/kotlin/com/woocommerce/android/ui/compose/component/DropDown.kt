package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> WCExposedDropDown(
    items: Set<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    currentValue: T = items.first(),
    mapper: (T) -> String = { it.toString() },
    focusRequester: FocusRequester = FocusRequester(),
    fillWidth: Boolean = false
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
                modifier = Modifier
                    .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .focusRequester(focusRequester)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = mapper(item),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            selectedText = item
                            expanded = false
                            onSelected(selectedText)
                        },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun WCExposedDropDownPreview() {
    WooThemeWithBackground {
        WCExposedDropDown(
            items = setOf("Item 1", "Item 2", "Item 3"),
            onSelected = { /* Handle selection */ },
            currentValue = "Item 1",
            mapper = { it },
            modifier = Modifier.safeContentPadding()
        )
    }
}
