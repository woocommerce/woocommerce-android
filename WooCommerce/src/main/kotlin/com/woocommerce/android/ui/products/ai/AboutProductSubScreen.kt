package com.woocommerce.android.ui.products.ai

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheetLayout
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.UiState
import com.woocommerce.android.ui.products.ai.AiTone.Casual
import kotlinx.coroutines.launch

@Composable
fun AboutProductSubScreen(viewModel: AboutProductSubViewModel, modifier: Modifier) {
    viewModel.state.observeAsState().value?.let { state ->
        AboutProductSubScreen(
            state,
            viewModel::onProductFeaturesUpdated,
            viewModel::onDoneClick,
            viewModel::onNewToneSelected,
            modifier
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AboutProductSubScreen(
    state: UiState,
    onProductFeaturesUpdated: (String) -> Unit,
    onCreateProductDetails: () -> Unit,
    onToneSelected: (AiTone) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded }
    )
    val configuration = LocalConfiguration.current

    WCModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetContent = {
            AiToneBottomSheetContent(
                aiTones = AiTone.values().toList(),
                selectedTone = state.selectedAiTone,
                onToneSelected = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onToneSelected(it)
                }
            )
        }
    ) {
        Column(
            modifier = modifier
                .background(MaterialTheme.colors.surface)
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
            ) {
                Text(
                    text = stringResource(id = R.string.product_creation_ai_about_product_title),
                    style = MaterialTheme.typography.h5,
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
                )
                Text(
                    text = stringResource(id = R.string.product_creation_ai_about_product_subtitle),
                    style = MaterialTheme.typography.subtitle1,
                    color = colorResource(id = R.color.color_on_surface_medium)
                )
                Column(
                    modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.major_150)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
                ) {
                    Text(
                        text = state.productName,
                        style = MaterialTheme.typography.body2,
                    )
                    Box {
                        if (state.productFeatures.isEmpty()) {
                            Text(
                                text = stringResource(id = R.string.ai_product_creation_add_name_keywords_placeholder),
                                style = MaterialTheme.typography.body1,
                                color = colorResource(id = R.color.color_on_surface_medium),
                                modifier = Modifier.padding(
                                    horizontal = dimensionResource(id = R.dimen.major_100),
                                    vertical = dimensionResource(id = R.dimen.major_150)
                                )
                            )
                        }
                        WCOutlinedTextField(
                            value = state.productFeatures,
                            onValueChange = onProductFeaturesUpdated,
                            label = "",
                            textFieldModifier = Modifier.height(
                                dimensionResource(id = R.dimen.multiline_textfield_height)
                            )
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.product_creation_ai_about_product_edit_text_caption),
                        style = MaterialTheme.typography.caption,
                        color = colorResource(id = R.color.color_on_surface_medium),
                    )
                }
                WCTextButton(
                    modifier = Modifier
                        .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                        .offset(x = (-16).dp),
                    contentPadding = PaddingValues(dimensionResource(id = R.dimen.major_100)),
                    onClick = {
                        coroutineScope.launch {
                            if (modalSheetState.isVisible) {
                                modalSheetState.hide()
                            } else {
                                modalSheetState.show()
                            }
                        }
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.product_creation_ai_about_product_set_tone),
                        style = MaterialTheme.typography.body1,
                        color = colorResource(id = R.color.color_primary)
                    )
                }

                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // Make button part of the scrollable content on landscape
                    ContinueButton(
                        onClick = onCreateProductDetails,
                        enabled = state.productFeatures.isNotBlank()
                    )
                }
            }

            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                // Stick button to the bottom on portrait mode
                ContinueButton(
                    onClick = onCreateProductDetails,
                    enabled = state.productFeatures.isNotBlank()
                )
            }
        }
    }
}

@Composable
private fun ContinueButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WCColoredButton(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled
    ) {
        Text(text = stringResource(id = R.string.product_creation_ai_about_product_continue_button))
    }
}

@Composable
private fun AiToneBottomSheetContent(
    aiTones: List<AiTone>,
    selectedTone: AiTone,
    onToneSelected: (AiTone) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
        Text(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .align(Alignment.CenterHorizontally),
            text = stringResource(id = R.string.product_creation_ai_tone_title),
            style = MaterialTheme.typography.h6,
        )
        Divider()
        Text(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
            text = stringResource(id = R.string.product_creation_ai_tone_description),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        aiTones.forEachIndexed { index, tone ->
            Row(
                modifier = Modifier
                    .clickable { onToneSelected(tone) }
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.major_100))
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(id = tone.displayName),
                    style = MaterialTheme.typography.subtitle1,
                )
                if (selectedTone == tone) {
                    Icon(
                        imageVector = Filled.Check,
                        contentDescription = stringResource(
                            id = R.string.product_creation_ai_tone_selected_content_desc
                        ),
                        tint = colorResource(id = R.color.color_primary)
                    )
                }
            }
            if (index < aiTones.size - 1) {
                Divider(modifier = Modifier.padding(dimensionResource(id = R.dimen.minor_75)))
            }
        }
    }
}

@ExperimentalFoundationApi
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun AboutProductSubScreenPreview() {
    AboutProductSubScreen(
        state = UiState(
            productName = "productName",
            productFeatures = "productFeatures",
            selectedAiTone = Casual
        ),
        onProductFeaturesUpdated = {},
        onCreateProductDetails = {},
        onToneSelected = {},
        modifier = Modifier
    )
}
