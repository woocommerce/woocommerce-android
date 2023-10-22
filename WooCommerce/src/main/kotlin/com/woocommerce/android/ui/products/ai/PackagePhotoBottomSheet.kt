package com.woocommerce.android.ui.products.ai

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest.Builder
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.mediapicker.MediaPickerDialog
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Generating
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.GeneratingFailure
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Initial
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Scanning
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.ScanningFailure
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Success
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.Keyword

@Composable
fun PackagePhotoBottomSheet(viewModel: PackagePhotoViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        ProductFromPackagePhoto(
            viewState = state,
            modifier = Modifier,
            viewModel::onKeywordChanged,
            viewModel::onRegenerateTapped,
            viewModel::onEditPhotoTapped,
            onMediaPickerDialogDismissed = viewModel::onMediaLibraryDialogDismissed,
            onDevicePickerRequested = viewModel::onDevicePickerRequested,
            onCameraRequested = viewModel::onCameraRequested,
            onWpMediaLibraryRequested = viewModel::onWpMediaLibraryRequested
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ProductFromPackagePhoto(
    viewState: ViewState,
    modifier: Modifier,
    onKeywordChanged: (Int, Keyword) -> Unit,
    onRegenerateTapped: () -> Unit,
    onEditPhotoTapped: () -> Unit,
    onMediaPickerDialogDismissed: () -> Unit,
    onDevicePickerRequested: () -> Unit,
    onCameraRequested: () -> Unit,
    onWpMediaLibraryRequested: () -> Unit
) {
    if (viewState.isMediaPickerDialogVisible) {
        MediaPickerDialog(
            onMediaPickerDialogDismissed,
            onDevicePickerRequested,
            onCameraRequested,
            onWpMediaLibraryRequested
        )
    }

    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = dimen.minor_100),
            topEnd = dimensionResource(id = dimen.minor_100)
        ),
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(rememberNestedScrollInteropConnection())
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
            modifier = modifier
                .background(MaterialTheme.colors.surface)
                .padding(dimensionResource(id = dimen.major_100))
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))

            ProductImage(viewState, onEditPhotoTapped)

            Spacer(Modifier)

            when (viewState.state) {
                Initial -> {}
                Scanning -> {
                    LoadingNameAndDescription()
                    Spacer(Modifier)
                    LoadingKeywords()
                }
                Generating -> {
                    LoadingNameAndDescription()
                    Spacer(Modifier)
                    Keywords(viewState, onKeywordChanged, onRegenerateTapped, false)
                }
                Success -> {
                    NameAndDescription(viewState)
                    Spacer(Modifier)
                    Keywords(viewState, onKeywordChanged, onRegenerateTapped, true)
                }
                is GeneratingFailure -> TODO()
                is ScanningFailure -> TODO()
            }
        }
    }
}

@Composable
private fun ProductImage(viewState: ViewState, onEditPhotoTapped: () -> Unit) {
    Box(
        modifier = Modifier
            .border(
                width = dimensionResource(id = dimen.minor_10),
                color = colorResource(id = color.divider_color),
                shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100))
            )
            .fillMaxWidth()
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(
                    horizontal = dimensionResource(id = dimen.major_200),
                    vertical = dimensionResource(id = dimen.major_100)
                )
        ) {
            val (image, button) = createRefs()

            SubcomposeAsyncImage(
                modifier = Modifier
                    .animateContentSize(animationSpec = tween(durationMillis = 500))
                    .defaultMinSize(minWidth = dimensionResource(id = dimen.image_major_100))
                    .constrainAs(image) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    },
                model = Builder(LocalContext.current)
                    .data(viewState.imageUrl)
                    .crossfade(true)
                    .placeholder(drawable.ic_product)
                    .error(drawable.img_woo_generic_error)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            ) {
                val state = painter.state
                if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(dimensionResource(id = dimen.progress_bar_mid))
                        )
                    }
                } else {
                    SubcomposeAsyncImageContent()
                }
            }

            WCColoredButton(
                modifier = Modifier
                    .constrainAs(button) {
                        centerAround(image.top)
                        centerAround(image.end)
                    }
                    .size(dimensionResource(id = dimen.button_height_major_100)),
                shape = CircleShape,
                onClick = onEditPhotoTapped,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colorResource(id = color.woo_white),
                )
            }
        }
    }
}

@Composable
fun NameAndDescription(
    viewState: ViewState
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
    ) {
        val sectionsBorder = Modifier.border(
            width = dimensionResource(id = dimen.minor_10),
            color = colorResource(id = color.divider_color),
            shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100))
        )

        Text(
            text = stringResource(id = string.product_creation_ai_preview_name_section),
            style = MaterialTheme.typography.body2
        )
        Text(
            text = viewState.title,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
                .padding(dimensionResource(id = dimen.major_100))
        )

        Spacer(Modifier)

        Text(
            text = stringResource(id = string.product_creation_ai_preview_description_section),
            style = MaterialTheme.typography.body2
        )
        Text(
            text = viewState.description,
            modifier = Modifier
                .fillMaxWidth()
                .then(sectionsBorder)
                .padding(dimensionResource(id = dimen.major_100))
        )
    }
}

@Composable
private fun Keywords(
    viewState: ViewState,
    onKeywordChanged: (Int, Keyword) -> Unit,
    onRegenerateTapped: () -> Unit,
    areKeywordsEnabled: Boolean
) {
    val sectionsBorder = Modifier.border(
        width = dimensionResource(id = dimen.minor_10),
        color = colorResource(id = color.divider_color),
        shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100))
    )

    Text(
        text = stringResource(id = string.product_creation_package_photo_keywords_section),
        style = MaterialTheme.typography.body2
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(sectionsBorder)
    ) {
        if (areKeywordsEnabled) {
            WCColoredButton(
                modifier = Modifier
                    .padding(dimensionResource(id = dimen.major_100))
                    .fillMaxWidth(),
                text = stringResource(string.ai_product_name_sheet_regenerate_button),
                onClick = onRegenerateTapped
            )
        } else {
            WCColoredButton(
                modifier = Modifier
                    .padding(dimensionResource(id = dimen.major_100))
                    .fillMaxWidth(),
                enabled = false,
                onClick = { },
                content = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(dimensionResource(id = dimen.major_150)),
                        color = colorResource(id = color.color_on_surface_disabled)
                    )
                }
            )
        }

        Text(
            text = stringResource(id = string.product_creation_package_photo_keywords_info),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = color.color_on_surface_medium),
            modifier = Modifier.padding(horizontal = dimensionResource(id = dimen.major_100))
        )

        viewState.keywords.forEachIndexed { index, keyword ->
            KeywordListItem(index, keyword.title, keyword.isChecked, onKeywordChanged, areKeywordsEnabled)

            if (index < viewState.keywords.lastIndex) {
                Divider(
                    color = colorResource(id = color.divider_color),
                    thickness = dimensionResource(id = dimen.minor_10)
                )
            }
        }
    }
}

@Composable
fun KeywordListItem(
    index: Int,
    title: String,
    isSelected: Boolean,
    onKeywordChanged: (Int, Keyword) -> Unit,
    isEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .padding(
                horizontal = dimensionResource(id = dimen.major_100),
                vertical = dimensionResource(id = dimen.minor_75)
            )
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_100))
    ) {
        val selectionDrawable = when (isSelected) {
            true -> drawable.ic_rounded_chcekbox_checked
            false -> drawable.ic_rounded_chcekbox_unchecked
        }

        BasicTextField(
            value = title,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f),
            textStyle = MaterialTheme.typography.body1,
            enabled = isEnabled,
            onValueChange = {
                onKeywordChanged(index, Keyword(it, isSelected))
            },
        )

        Crossfade(
            targetState = selectionDrawable,
            label = "",
        ) { icon ->
            val onClick = {
                onKeywordChanged(index, Keyword(title, !isSelected))
            }
            IconButton(
                onClick = onClick,
                enabled = isEnabled,
            ) {
                Image(
                    painter = painterResource(id = icon), contentDescription = "",
                    colorFilter = if (isEnabled)
                            null
                        else
                            ColorFilter.tint(colorResource(id = color.color_on_surface_disabled))
                )
            }
        }
    }
}

@Composable
fun LoadingNameAndDescription() {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
    ) {
        val sectionsBorder = Modifier.border(
            width = dimensionResource(id = dimen.minor_10),
            color = colorResource(id = color.divider_color),
            shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100))
        )

        Text(
            text = stringResource(id = string.product_creation_ai_preview_name_section),
            style = MaterialTheme.typography.body2
        )

        Box(modifier = Modifier
            .fillMaxWidth()
            .then(sectionsBorder)
        ) {
            SkeletonView(
                modifier = Modifier
                    .padding(dimensionResource(id = dimen.major_100))
                    .fillMaxWidth(0.8f)
                    .height(dimensionResource(id = dimen.major_100))
            )
        }

        Spacer(Modifier)

        Text(
            text = stringResource(id = string.product_creation_ai_preview_description_section),
            style = MaterialTheme.typography.body2
        )

        Box(modifier = Modifier
            .fillMaxWidth()
            .then(sectionsBorder)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
                modifier = Modifier
                    .padding(dimensionResource(id = dimen.major_100))
                    .fillMaxWidth()
            ) {

                SkeletonView(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(dimensionResource(id = dimen.major_100))
                )
                SkeletonView(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(dimensionResource(id = dimen.major_100))
                )
                SkeletonView(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(dimensionResource(id = dimen.major_100))
                )
            }
        }
    }
}

@Composable
private fun LoadingKeywords() {
    val sectionsBorder = Modifier.border(
        width = dimensionResource(id = dimen.minor_10),
        color = colorResource(id = color.divider_color),
        shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100))
    )

    Text(
        text = stringResource(id = string.product_creation_package_photo_keywords_section),
        style = MaterialTheme.typography.body2
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(sectionsBorder)
    ) {
        WCColoredButton(
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100))
                .fillMaxWidth(),
            enabled = false,
            onClick = { },
            content = {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(id = dimen.major_150)),
                    color = colorResource(id = color.color_on_surface_disabled)
                )
            }
        )
        Text(
            text = stringResource(id = string.product_creation_package_photo_keywords_info),
            style = MaterialTheme.typography.caption,
            color = colorResource(id = color.color_on_surface_medium),
            modifier = Modifier.padding(horizontal = dimensionResource(id = dimen.major_100))
        )

        LoadingListItem()

        Divider(
            color = colorResource(id = color.divider_color),
            thickness = dimensionResource(id = dimen.minor_10)
        )

        LoadingListItem()

        Divider(
            color = colorResource(id = color.divider_color),
            thickness = dimensionResource(id = dimen.minor_10)
        )

        LoadingListItem()
    }
}

@Composable
private fun LoadingListItem() {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        SkeletonView(
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100))
                .fillMaxWidth(0.8f)
                .align(Alignment.CenterVertically)
                .height(dimensionResource(id = dimen.major_100))
        )
        SkeletonView(
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100))
                .width(dimensionResource(id = dimen.major_150))
                .height(dimensionResource(id = dimen.major_150))
        )
    }
}


@Preview
@Composable
fun PreviewLoading() {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100)),
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .padding(dimensionResource(id = dimen.major_100))
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
    ) {
        BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
        LoadingNameAndDescription()
        Spacer(Modifier)
        LoadingKeywords()
    }
}


@Preview
@Composable
fun PreviewProductFromPackagePhoto() {
    ProductFromPackagePhoto(
        viewState = ViewState(
            title = "Title",
            description = "Description",
            keywords = listOf(Keyword("Keyword 1", true), Keyword("Keyword 2", false)),
            state = Initial,
            imageUrl = ""
        ),
        modifier = Modifier,
        onKeywordChanged = { _, _ -> },
        {},
        {},
        {},
        {},
        {},
        {}
    )
}
