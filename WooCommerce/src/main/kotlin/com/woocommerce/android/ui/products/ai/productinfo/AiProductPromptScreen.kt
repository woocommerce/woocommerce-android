package com.woocommerce.android.ui.products.ai.productinfo

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.AiTone
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.AiProductPromptState

@Composable
fun AiProductPromptScreen(viewModel: AiProductPromptViewModel) {
    BackHandler(onBack = viewModel::onBackButtonClick)

    viewModel.state.observeAsState().value?.let { state ->
        AiProductPromptScreen(
            uiState = state,
            onBackButtonClick = viewModel::onBackButtonClick,
            onPromptUpdated = viewModel::onPromptUpdated,
            onReadTextFromProductPhoto = viewModel::onReadTextFromProductPhoto,
            onGenerateProductClicked = viewModel::onGenerateProductClicked
        )
    }
}

@Composable
fun AiProductPromptScreen(
    uiState: AiProductPromptState,
    onBackButtonClick: () -> Unit,
    onPromptUpdated: (String) -> Unit,
    onReadTextFromProductPhoto: () -> Unit,
    onGenerateProductClicked: () -> Unit
) {
    val orientation = LocalConfiguration.current.orientation

    @Composable
    fun GenerateProductButton(modifier: Modifier = Modifier) {
        WCColoredButton(
            enabled = uiState.productPrompt.isNotEmpty(),
            onClick = onGenerateProductClicked,
            modifier = modifier
                .fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.ai_product_creation_generate_details_button))
        }
    }

    Scaffold(
        topBar = {
            Toolbar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationButtonClick = onBackButtonClick,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                Text(
                    text = stringResource(id = R.string.ai_product_creation_product_prompt_title),
                    style = MaterialTheme.typography.h5
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                Text(
                    text = stringResource(id = R.string.ai_product_creation_product_prompt_subtitle),
                    style = MaterialTheme.typography.subtitle1,
                    color = colorResource(id = R.color.color_on_surface_medium)
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_250)))

                ProductPromptTextField(
                    productPrompt = uiState.productPrompt,
                    onPromptUpdated = onPromptUpdated,
                    onReadTextFromProductPhoto = onReadTextFromProductPhoto,
                )

                Spacer(modifier = Modifier.weight(1f))

                // Button will scroll with the rest of UI on landscape mode, or... (see below)
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    GenerateProductButton(Modifier.padding(top = 16.dp))
                }
            }

            // Button will stick to the bottom on portrait mode
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                GenerateProductButton()
            }
        }
    }
}

@Composable
private fun ProductPromptTextField(
    productPrompt: String,
    onPromptUpdated: (String) -> Unit,
    onReadTextFromProductPhoto: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isFocused) {
                        colorResource(id = R.color.color_primary)
                    } else {
                        colorResource(id = R.color.divider_color)
                    },
                    shape = RoundedCornerShape(10.dp)
                )
                .clip(RoundedCornerShape(10.dp))
        ) {
            Box {
                if (productPrompt.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.ai_product_creation_prompt_placeholder),
                        style = MaterialTheme.typography.body1,
                        color = Color.Gray,
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(id = R.dimen.major_100),
                            vertical = dimensionResource(id = R.dimen.major_150)
                        )
                    )
                }

                WCOutlinedTextField(
                    value = productPrompt,
                    onValueChange = onPromptUpdated,
                    label = "", // Can't use label here as it breaks the visual design.
                    placeholderText = "", // Uses Text() above instead.
                    textFieldModifier = Modifier.height(dimensionResource(id = R.dimen.multiline_textfield_height)),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Transparent, // Remove outline and use Column's border instead.
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                )
            }

            Divider()

            WCTextButton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(id = R.dimen.minor_50)),
                onClick = onReadTextFromProductPhoto,
                icon = ImageVector.vectorResource(id = R.drawable.ic_gridicons_camera_primary),
                allCaps = false,
                text = stringResource(id = R.string.ai_product_creation_read_text_from_photo_button),
            )
        }
    }
}

@Preview
@Composable
private fun AiProductPromptScreenPreview() {
    AiProductPromptScreen(
        uiState = AiProductPromptState(
            productPrompt = "Product prompt test",
            selectedAiTone = AiTone.Casual
        ),
        onBackButtonClick = {},
        onPromptUpdated = {},
        onReadTextFromProductPhoto = {},
        onGenerateProductClicked = {}
    )
}
