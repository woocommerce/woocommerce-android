package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosCustomAmountNameScreen(
    value: String,
    onNameChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val saveName = {
        keyboardController?.hide()
        onSaveClicked()
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    WooPosCustomAmountLayout(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceBright),
        title = stringResource(R.string.woopos_custom_amount_dialog_name_label),
        onBackClicked = onBackClicked,
        onSaveClicked = saveName,
        isEditor = true,
    ) { contentModifier ->
        Box(modifier = contentModifier, contentAlignment = Alignment.Center) {
            WooPosInputField(
                modifier = Modifier.focusRequester(focusRequester),
                value = value,
                onValueChange = onNameChanged,
                label = stringResource(R.string.woopos_custom_amount_dialog_name_placeholder),
                textStyle = WooPosTypography.Heading,
                textColor = MaterialTheme.colorScheme.onSurface,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { saveName() }),
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosCustomAmountNameScreenPreview() {
    WooPosTheme {
        WooPosCustomAmountNameScreen(
            value = "This is a name example",
            onNameChanged = {},
            onSaveClicked = {},
            onBackClicked = {},
        )
    }
}
