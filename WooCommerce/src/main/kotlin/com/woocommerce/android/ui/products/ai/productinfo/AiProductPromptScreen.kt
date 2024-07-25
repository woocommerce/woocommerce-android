package com.woocommerce.android.ui.products.ai.productinfo

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProgressIndicatorDefaults
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.mediapicker.MediaPickerDialog
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.products.ai.AiTone
import com.woocommerce.android.ui.products.ai.components.FullScreenImageViewer
import com.woocommerce.android.ui.products.ai.components.ImageAction
import com.woocommerce.android.ui.products.ai.components.SelectImageSection
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.AiProductPromptState
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.PromptSuggestionBar
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import kotlin.math.roundToInt

@Composable
fun AiProductPromptScreen(viewModel: AiProductPromptViewModel) {
    BackHandler(onBack = viewModel::onBackButtonClick)

    viewModel.state.observeAsState().value?.let { state ->
        AiProductPromptScreen(
            uiState = state,
            onBackButtonClick = viewModel::onBackButtonClick,
            onPromptUpdated = viewModel::onPromptUpdated,
            onReadTextFromProductPhoto = viewModel::onAddImageForScanning,
            onGenerateProductClicked = viewModel::onGenerateProductClicked,
            onToneSelected = viewModel::onToneSelected,
            onMediaPickerDialogDismissed = viewModel::onMediaPickerDialogDismissed,
            onMediaLibraryRequested = viewModel::onMediaLibraryRequested,
            onImageActionSelected = viewModel::onImageActionSelected
        )

        if (state.showImageFullScreen && state.selectedImage != null) {
            FullScreenImageViewer(
                state.selectedImage,
                viewModel::onImageFullScreenDismissed
            )
        }
    }
}

@Composable
fun AiProductPromptScreen(
    uiState: AiProductPromptState,
    onBackButtonClick: () -> Unit,
    onPromptUpdated: (String) -> Unit,
    onReadTextFromProductPhoto: () -> Unit,
    onGenerateProductClicked: () -> Unit,
    onToneSelected: (AiTone) -> Unit,
    onMediaPickerDialogDismissed: () -> Unit,
    onMediaLibraryRequested: (DataSource) -> Unit,
    onImageActionSelected: (ImageAction) -> Unit
) {
    val orientation = LocalConfiguration.current.orientation
    val scrollState = rememberScrollState()

    @Composable
    fun GenerateProductButton(
        modifier: Modifier = Modifier
    ) {
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
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                Text(
                    text = stringResource(id = R.string.ai_product_creation_product_prompt_title),
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                Text(
                    text = stringResource(id = R.string.ai_product_creation_product_prompt_subtitle),
                    style = MaterialTheme.typography.subtitle1,
                    color = colorResource(id = R.color.color_on_surface_medium)
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_250)))

                ProductPromptTextField(
                    state = uiState,
                    onPromptUpdated = onPromptUpdated,
                    scrollState = scrollState,
                )
                ScanProductPhotoButton(
                    state = uiState,
                    onReadTextFromProductPhoto = onReadTextFromProductPhoto,
                    onImageActionSelected = onImageActionSelected,
                    modifier = Modifier.padding(top = 8.dp)
                )
                ToneDropDown(
                    tone = uiState.selectedTone,
                    onToneSelected = onToneSelected,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Button will scroll with the rest of UI on landscape mode, or... (see below)
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Divider()
                    GenerateProductButton(Modifier.padding(16.dp))
                }
            }

            // Button will stick to the bottom on portrait mode
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                Divider()
                GenerateProductButton(
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                        horizontal = 16.dp
                    )
                )
            }
        }
    }
    if (uiState.isMediaPickerDialogVisible) {
        MediaPickerDialog(
            onMediaPickerDialogDismissed,
            onMediaLibraryRequested
        )
    }
}

@Composable
private fun ToneDropDown(
    tone: AiTone,
    onToneSelected: (AiTone) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.product_creation_ai_tone_title),
            style = MaterialTheme.typography.subtitle1,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(id = tone.displayName),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.primary
        )

        Box {
            var isMenuExpanded by remember { mutableStateOf(false) }
            IconButton(
                onClick = { isMenuExpanded = true }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_gridicons_double_chevron),
                    contentDescription = stringResource(id = R.string.dashboard_filter_menu_content_description),
                    tint = MaterialTheme.colors.primary
                )
            }

            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                modifier = Modifier
                    .defaultMinSize(minWidth = 250.dp)
            ) {
                AiTone.entries.forEach {
                    DropdownMenuItem(
                        onClick = {
                            onToneSelected(it)
                            isMenuExpanded = false
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.minor_100))
                        ) {
                            Text(text = stringResource(id = it.displayName))
                            Spacer(modifier = Modifier.weight(1f))
                            if (tone == it) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(id = androidx.compose.ui.R.string.selected),
                                    tint = MaterialTheme.colors.primary
                                )
                            } else {
                                Spacer(modifier = Modifier.size(dimensionResource(R.dimen.image_minor_50)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductPromptTextField(
    state: AiProductPromptState,
    onPromptUpdated: (String) -> Unit,
    scrollState: ScrollState
) {
    val coroutineScope = rememberCoroutineScope()
    var isFocused by remember { mutableStateOf(false) }
    var scrollToPosition by remember { mutableFloatStateOf(0F) }

    Column(
        modifier = Modifier.onGloballyPositioned {
            scrollToPosition = it.positionInParent().y
        }
    ) {
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
                if (state.productPrompt.isEmpty()) {
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
                    value = state.productPrompt,
                    onValueChange = onPromptUpdated,
                    label = "", // Can't use label here as it breaks the visual design.
                    placeholderText = "", // Uses Text() above instead.
                    minLines = 3,
                    maxLines = 6,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = Color.Transparent, // Remove outline and use Column's border instead.
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (isFocused && FeatureFlag.PRODUCT_CREATION_WITH_AI_V2_M3.isEnabled()) {
                                coroutineScope.launch {
                                    @Suppress("MagicNumber")
                                    delay(200) // Delay to ensure advice box is shown before scrolling.
                                    scrollState.animateScrollTo(scrollToPosition.roundToInt())
                                }
                            }
                        }
                )
            }
        }
        AnimatedVisibility(
            (isFocused || state.productPrompt.isNotEmpty()) &&
                FeatureFlag.PRODUCT_CREATION_WITH_AI_V2_M3.isEnabled()
        ) {
            PromptSuggestions(
                promptSuggestionBarState = state.promptSuggestionBarState,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ScanProductPhotoButton(
    state: AiProductPromptState,
    onReadTextFromProductPhoto: () -> Unit,
    onImageActionSelected: (ImageAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        state.isScanningImage -> ImageScanning()
        else -> SelectImageSection(
            image = state.selectedImage,
            onImageActionSelected = onImageActionSelected,
            onReadTextFromProductPhoto = onReadTextFromProductPhoto,
            subtitle = when {
                state.selectedImage == null -> stringResource(id = R.string.ai_product_creation_image_scan_subtitle)
                else -> stringResource(id = R.string.ai_product_creation_image_selected_subtitle)
            },
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(color = colorResource(id = R.color.ai_generated_text_background))
        )
    }
    if (state.noTextDetectedMessage) {
        InformativeMessage(
            stringResource(id = R.string.product_creation_package_photo_no_text_detected)
        )
    }
}

@Composable
private fun PromptSuggestions(
    promptSuggestionBarState: PromptSuggestionBar,
    modifier: Modifier = Modifier
) {
    val animatedProgress = animateFloatAsState(
        targetValue = promptSuggestionBarState.progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = ""
    ).value
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
            .background(colorResource(id = R.color.ai_generated_text_background))
            .padding(16.dp)
    ) {
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))),
            color = colorResource(id = promptSuggestionBarState.progressBarColorRes)
        )
        Text(
            text = annotatedStringRes(promptSuggestionBarState.messageRes),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun ImageScanning() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .wrapContentWidth()
                .padding(end = 16.dp)
        )
        Text(
            text = stringResource(id = R.string.ai_product_creation_scanning_image),
            style = MaterialTheme.typography.subtitle1,
        )
    }
}

@Composable
private fun InformativeMessage(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)))
            .background(
                colorResource(id = R.color.tag_bg_main)
            )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_info_outline_20dp),
            contentDescription = null,
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .size(dimensionResource(id = R.dimen.major_150)),
            tint = colorResource(id = R.color.tag_text_main),
        )
        Text(
            text = message,
            color = colorResource(id = R.color.tag_text_main),
            modifier = Modifier
                .weight(1f)
                .padding(
                    top = dimensionResource(id = R.dimen.major_100),
                    end = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.major_100)
                )
        )
    }
}

@Preview
@Composable
private fun AiProductPromptScreenPreview() {
    AiProductPromptScreen(
        uiState = AiProductPromptState(
            productPrompt = "Product prompt test",
            selectedTone = AiTone.Casual,
            isMediaPickerDialogVisible = false,
            selectedImage = null,
            isScanningImage = false,
            showImageFullScreen = false,
            noTextDetectedMessage = false,
            promptSuggestionBarState = PromptSuggestionBar(
                progressBarColorRes = R.color.linear_progress_background_gray,
                messageRes = R.string.ai_product_creation_prompt_suggestion_initial,
                progress = 0.1f
            ),
        ),
        onBackButtonClick = {},
        onPromptUpdated = {},
        onReadTextFromProductPhoto = {},
        onGenerateProductClicked = {},
        onToneSelected = {},
        onMediaPickerDialogDismissed = {},
        onMediaLibraryRequested = {},
        onImageActionSelected = {}
    )
}

@Preview
@Composable
private fun AiProductPromptScreenWithErrorPreview() {
    AiProductPromptScreen(
        uiState = AiProductPromptState(
            productPrompt = "Product prompt test",
            selectedTone = AiTone.Casual,
            isMediaPickerDialogVisible = false,
            selectedImage = null,
            isScanningImage = false,
            showImageFullScreen = false,
            noTextDetectedMessage = true,
            promptSuggestionBarState = PromptSuggestionBar(
                progressBarColorRes = R.color.linear_progress_background_gray,
                messageRes = R.string.ai_product_creation_prompt_suggestion_initial,
                progress = 0.1f
            ),
        ),
        onBackButtonClick = {},
        onPromptUpdated = {},
        onReadTextFromProductPhoto = {},
        onGenerateProductClicked = {},
        onToneSelected = {},
        onMediaPickerDialogDismissed = {},
        onMediaLibraryRequested = {},
        onImageActionSelected = {}
    )
}
