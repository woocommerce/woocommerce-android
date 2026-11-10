package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosInputField
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.util.WooPosTestTags

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
        modifier = modifier,
        title = stringResource(R.string.woopos_custom_amount_dialog_name_label),
        onBackClicked = onBackClicked,
        onSaveClicked = saveName,
    ) { contentModifier ->
        Box(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = WooPosSpacing.XLarge.value),
            contentAlignment = Alignment.Center,
        ) {
            WooPosInputField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .testTag(WooPosTestTags.CUSTOM_AMOUNT_NAME_INPUT),
                value = value,
                onValueChange = onNameChanged,
                label = stringResource(R.string.woopos_custom_amount_dialog_name_placeholder),
                textStyle = WooPosTypography.Heading,
                textColor = MaterialTheme.colorScheme.onSurface,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { saveName() }),
                preselectText = true,
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
