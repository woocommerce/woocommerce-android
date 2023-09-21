package com.woocommerce.android.ui.products.ai

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ProductNameSubScreen(viewModel: ProductNameSubViewModel, modifier: Modifier) {
    viewModel.state.observeAsState().value?.let { state ->
        Column(
            modifier = modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxWidth()
        ) {
            ProductNameForm(
                enteredName = state.name,
                onProductNameChanged = {},
                onSuggestNameClicked = {},
                onContinueClicked = {}
            )
        }
    }
}

@Composable
fun ProductNameForm(
    enteredName: String,
    onProductNameChanged: (String) -> Unit,
    onSuggestNameClicked: () -> Unit,
    onContinueClicked: () -> Unit
) {
    val orientation = LocalConfiguration.current.orientation

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(id = R.string.ai_product_creation_add_name_title),
                style = MaterialTheme.typography.h5
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

            Text(
                text = stringResource(id = R.string.ai_product_creation_add_name_subtitle),
                style = MaterialTheme.typography.body1
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_250)))

            WCOutlinedTextField(
                value = enteredName,
                onValueChange = onProductNameChanged,
                label = stringResource(id = R.string.ai_product_creation_add_name_keywords_label),
                placeholderText = stringResource(id = R.string.ai_product_creation_add_name_keywords_placeholder),
                textFieldModifier = Modifier.height(dimensionResource(id = R.dimen.multiline_textfield_height))
            )

            WCOutlinedButton(
                onClick = onSuggestNameClicked,
                text = stringResource(id = R.string.ai_product_creation_add_name_suggest_name_button),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ai_share_button),
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Button will scroll with the rest of UI on landscape mode, or... (see below)
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                WCColoredButton(
                    onClick = onContinueClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensionResource(id = R.dimen.major_100))
                ) {
                    Text(text = stringResource(id = R.string.continue_button))
                }
            }
        }

        // Button will stick to the bottom on portrait mode
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            WCColoredButton(
                onClick = onContinueClicked,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.major_100))
            ) {
                Text(text = stringResource(id = R.string.continue_button))
            }
        }
    }
}

@Preview
@Composable
fun ProductNamePreview() {
    WooThemeWithBackground {
        ProductNameForm(
            enteredName = "Everyday Elegance with Our Soft Black Tee",
            onProductNameChanged = {},
            onSuggestNameClicked = {},
            onContinueClicked = {}
        )
    }
}
