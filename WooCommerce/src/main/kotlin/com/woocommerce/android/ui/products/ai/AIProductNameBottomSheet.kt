package com.woocommerce.android.ui.products.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.products.ai.AIProductNameViewModel.ViewState.GenerationState

@Composable
fun AIProductNameBottomSheet(viewModel: AIProductNameViewModel) {

    viewModel.viewState.observeAsState().value?.let { state ->
        when (val generationState = state.generationState) {
            is GenerationState.Start -> StartLayout(
                keywords = state.keywords,
                onKeywordsChanged = viewModel::onProductKeywordsChanged,
                onGenerateButtonClicked = viewModel::onGenerateButtonClicked
            )
            is GenerationState.Generating -> GeneratingLayout(keywords = state.keywords)
            is GenerationState.Generated -> {
                if (generationState.hasError) {
                    ErrorLayout(
                        keywords = state.keywords,
                        onKeywordsChanged = viewModel::onProductKeywordsChanged,
                        onGenerateButtonClicked = viewModel::onGenerateButtonClicked
                    )
                } else {
                    ResultLayout(
                        keywords = state.keywords,
                        generatedProductName = state.generatedProductName,
                        onKeywordsChanged = viewModel::onProductKeywordsChanged,
                        onRegenerateButtonClicked = viewModel::onRegenerateButtonClicked,
                        onCopyButtonClicked = viewModel::onCopyButtonClicked,
                        onApplyButtonClicked = viewModel::onApplyButtonClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun MainLayout(
    keywords: String,
    enableProductKeywords: Boolean = true,
    onKeywordsChanged: (String) -> Unit = {},
    footer: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = dimen.minor_100),
            topEnd = dimensionResource(id = dimen.minor_100)
        )
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
            Column(
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = dimen.major_100)),
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = dimensionResource(id = dimen.major_100))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = drawable.ic_ai),
                            contentDescription = null, // decorative
                            modifier = Modifier
                                .padding(end = dimensionResource(id = dimen.minor_100))
                                .size(dimensionResource(id = dimen.major_150)),
                            tint = colorResource(id = color.woo_purple_60)
                        )
                        Text(
                            text = stringResource(id = string.ai_product_name_sheet_title),
                            style = MaterialTheme.typography.h6
                        )
                    }
                    Text(
                        text = stringResource(id = string.ai_product_name_sheet_subtitle),
                        style = MaterialTheme.typography.subtitle1,
                        modifier = Modifier
                            .padding(vertical = dimensionResource(id = dimen.minor_100))
                    )
                }

                Divider(
                    color = colorResource(id = color.divider_color),
                    thickness = dimensionResource(id = dimen.minor_10),
                    modifier = Modifier
                        .padding(vertical = dimensionResource(id = dimen.minor_100))
                )

                Column(
                    modifier = Modifier
                        .padding(dimensionResource(id = dimen.major_100))
                ) {
                    WCOutlinedTextField(
                        value = keywords,
                        onValueChange = onKeywordsChanged,
                        label = "",
                        enabled = enableProductKeywords,
                        textFieldModifier = Modifier.height(dimensionResource(id = dimen.major_400))
                    )

                    Text(
                        text = stringResource(id = string.ai_product_name_sheet_input_description),
                        style = MaterialTheme.typography.caption,
                        color = colorResource(id = color.color_on_surface_medium),
                        modifier = Modifier
                            .padding(vertical = dimensionResource(id = dimen.minor_100))
                    )

                    footer()
                }
            }
        }
    }
}

@Composable
private fun StartLayout(
    keywords: String,
    onKeywordsChanged: (String) -> Unit,
    onGenerateButtonClicked: () -> Unit
) {
    MainLayout(
        keywords = keywords,
        onKeywordsChanged = onKeywordsChanged
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_375)))

        WCColoredButton(
            enabled = keywords.isNotEmpty(),
            onClick = onGenerateButtonClicked,
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.ai_product_name_sheet_generate_button),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ai),
                    contentDescription = null,
                    tint = colorResource(id = R.color.woo_white)
                )
            }
        )
    }
}

@Composable
private fun GeneratingLayout(
    keywords: String
) {
    MainLayout(
        keywords = keywords,
        enableProductKeywords = false
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_300)))

        Column(
            modifier = Modifier
                .background(
                    color = colorResource(id = R.color.skeleton_compose_background),
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
                )
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            SkeletonView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(id = R.dimen.major_100))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            SkeletonView(
                modifier = Modifier
                    .width(dimensionResource(id = R.dimen.skeleton_text_large_width))
                    .height(dimensionResource(id = R.dimen.major_100))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }
}

@Composable
private fun ResultLayout(
    keywords: String,
    generatedProductName: String,
    onKeywordsChanged: (String) -> Unit,
    onRegenerateButtonClicked: () -> Unit,
    onCopyButtonClicked: () -> Unit,
    onApplyButtonClicked: () -> Unit
) {
    MainLayout(
        keywords = keywords,
        onKeywordsChanged = onKeywordsChanged
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        GeneratedTextLayout(
            generatedProductName = generatedProductName,
            onCopyButtonClicked = onCopyButtonClicked
        )

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10),
            modifier = Modifier
                .padding(vertical = dimensionResource(id = R.dimen.major_100))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            WCTextButton(
                enabled = keywords.isNotEmpty(),
                onClick = onRegenerateButtonClicked,
                modifier = Modifier.align(Alignment.CenterStart),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(id = R.color.color_on_surface)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.major_150))
                )
                Text(
                    modifier = Modifier.padding(start = dimensionResource(id = R.dimen.minor_100)),
                    text = stringResource(id = R.string.ai_product_name_sheet_regenerate_button)
                )
            }
            WCColoredButton(
                onClick = onApplyButtonClicked,
                modifier = Modifier
                    .align(Alignment.CenterEnd),
            ) {
                Text(
                    text = stringResource(id = R.string.apply),
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
                )
            }
        }
    }
}

@Composable
private fun GeneratedTextLayout(
    generatedProductName: String,
    onCopyButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(
                color = colorResource(id = R.color.ai_generated_text_background),
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
            )
            .padding(dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = generatedProductName,
            style = MaterialTheme.typography.body1
        )

        WCTextButton(
            modifier = Modifier.align(Alignment.End),
            onClick = onCopyButtonClicked,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorResource(id = R.color.color_on_surface_medium)
            )
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_150))
            )
            Text(
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.minor_100)),
                text = stringResource(id = R.string.copy)
            )
        }
    }
}

@Composable
private fun ErrorLayout(
    keywords: String,
    onKeywordsChanged: (String) -> Unit,
    onGenerateButtonClicked: () -> Unit
) {
    MainLayout(
        keywords = keywords,
        onKeywordsChanged = onKeywordsChanged
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colors.error,
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
                )
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = R.string.ai_product_name_sheet_error_message),
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onError,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        WCColoredButton(
            onClick = onGenerateButtonClicked,
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.ai_product_name_sheet_generate_button),
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ai),
                    contentDescription = null,
                    tint = colorResource(id = R.color.woo_white)
                )
            }
        )
    }
}

@Preview
@Composable
private fun StartLayoutPreview() {
    StartLayout("", onKeywordsChanged = {}, onGenerateButtonClicked = {})
}

@Preview
@Composable
private fun GeneratingLayoutPreview() {
    GeneratingLayout("")
}

@Preview
@Composable
private fun ResultLayoutPreview() {
    ResultLayout(
        keywords = "some keywords here",
        generatedProductName = "AI-generated product name here",
        onKeywordsChanged = {},
        onRegenerateButtonClicked = {},
        onCopyButtonClicked = {},
        onApplyButtonClicked = {}
    )
}

@Preview
@Composable
private fun ErrorLayoutPreview() {
    ErrorLayout(keywords = "", onKeywordsChanged = {}, onGenerateButtonClicked = {})
}
