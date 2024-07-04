package com.woocommerce.android.ui.products.ai.productinfo

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R
import com.woocommerce.android.mediapicker.MediaPickerDialog
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.AiProductPromptState
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.ImageAction
import com.woocommerce.android.ui.products.ai.productinfo.AiProductPromptViewModel.Tone
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import kotlin.enums.EnumEntries

@Composable
fun AiProductPromptScreen(viewModel: AiProductPromptViewModel) {
    BackHandler(onBack = viewModel::onBackButtonClick)

    viewModel.state.observeAsState().value?.let { state ->
        if (state.showImageFullScreen) {
            FullScreenImage(viewModel, state)
        } else {
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
        }
    }
}

@Composable
private fun FullScreenImage(
    viewModel: AiProductPromptViewModel,
    state: AiProductPromptState
) {
    BackHandler(onBack = viewModel::onImageFullScreenDismissed)
    Column {
        Toolbar(
            navigationIcon = Filled.Close,
            onNavigationButtonClick = viewModel::onImageFullScreenDismissed
        )
        AsyncImage(
            model = Builder(LocalContext.current)
                .data(state.mediaUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun AiProductPromptScreen(
    uiState: AiProductPromptState,
    onBackButtonClick: () -> Unit,
    onPromptUpdated: (String) -> Unit,
    onReadTextFromProductPhoto: () -> Unit,
    onGenerateProductClicked: () -> Unit,
    onToneSelected: (Tone) -> Unit,
    onMediaPickerDialogDismissed: () -> Unit,
    onMediaLibraryRequested: (DataSource) -> Unit,
    onImageActionSelected: (ImageAction) -> Unit
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
                    onReadTextFromProductPhoto = onReadTextFromProductPhoto,
                    onImageActionSelected = onImageActionSelected
                )

                ToneDropDown(
                    tone = uiState.selectedTone,
                    onToneSelected = onToneSelected
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
    if (uiState.isMediaPickerDialogVisible) {
        MediaPickerDialog(
            onMediaPickerDialogDismissed,
            onMediaLibraryRequested
        )
    }
}

@Composable
private fun ToneDropDown(
    tone: Tone,
    onToneSelected: (Tone) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.product_creation_ai_tone_title),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
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
                Tone.entries.forEach {
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
    onReadTextFromProductPhoto: () -> Unit,
    onImageActionSelected: (ImageAction) -> Unit
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

            when {
                state.isScanningImage -> {
                    // TODO() show progress while scanning image
                }

                state.mediaUri != null -> UploadedImageRow(
                    state.mediaUri,
                    onImageActionSelected
                )

                else -> {
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
    }
}

@Composable
private fun UploadedImageRow(
    mediaUri: String,
    onImageActionSelected: (ImageAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductThumbnail(
            imageUrl = mediaUri,
            contentDescription = stringResource(id = R.string.product_image_content_description),
            modifier = Modifier.padding(end = 16.dp)
        )
        Text(
            text = stringResource(id = R.string.ai_product_creation_image_uploaded),
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        )
        Spacer(modifier = Modifier.weight(1f))
        ImageActionsMenu(
            ImageAction.entries,
            onImageActionSelected
        )
    }
}

@Composable
private fun ImageActionsMenu(
    actions: EnumEntries<ImageAction>,
    onImageActionSelected: (ImageAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { showMenu = !showMenu }) {
            Icon(
                imageVector = Outlined.MoreVert,
                contentDescription = stringResource(R.string.more_menu),
                tint = colorResource(id = R.color.color_on_surface_high)
            )
        }
        DropdownMenu(
            offset = DpOffset(
                x = dimensionResource(id = R.dimen.major_100),
                y = 0.dp
            ),
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            actions.forEachIndexed { index, item ->
                DropdownMenuItem(
                    modifier = Modifier
                        .height(dimensionResource(id = R.dimen.major_175))
                        .width(200.dp),
                    onClick = {
                        showMenu = false
                        onImageActionSelected(item)
                    }
                ) {
                    Text(
                        text = stringResource(id = item.displayName),
                        color = when (item) {
                            ImageAction.Remove -> MaterialTheme.colors.error
                            else -> Color.Unspecified
                        }
                    )
                }
                if (index < actions.size - 1) {
                    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
                }
            }
        }
    }
}

@Preview
@Composable
private fun AiProductPromptScreenPreview() {
    AiProductPromptScreen(
        uiState = AiProductPromptState(
            productPrompt = "Product prompt test",
            selectedTone = Tone.Casual,
            isMediaPickerDialogVisible = false,
            mediaUri = null,
            isScanningImage = false,
            showImageFullScreen = false
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
