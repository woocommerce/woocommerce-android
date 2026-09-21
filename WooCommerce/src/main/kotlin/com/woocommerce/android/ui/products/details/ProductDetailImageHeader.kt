package com.woocommerce.android.ui.products.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.extensions.rememberPhotonImageRequest
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooCircularProgressIndicator
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.CircleInfo
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import kotlinx.coroutines.delay
import org.wordpress.android.util.PhotonUtils

@Composable
fun ProductDetailImageHeader(
    state: ProductDetailImageUiState,
    onImageClicked: () -> Unit,
    onAddImageClicked: () -> Unit,
    onImagesUnavailableClicked: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    if (state == ProductDetailImageUiState.Hidden) return

    Surface(
        color = WooTheme.colors.surface.bright,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.image_major_120)),
    ) {
        when (state) {
            ProductDetailImageUiState.Loading -> ProductDetailImageLoading()
            ProductDetailImageUiState.AddImage -> ProductDetailAddImage(onClick = onAddImageClicked)
            ProductDetailImageUiState.Unavailable -> ProductDetailImagesUnavailable(
                onClick = onImagesUnavailableClicked
            )
            is ProductDetailImageUiState.Gallery -> ProductDetailImageGallery(
                items = state.items,
                listState = listState,
                onImageClicked = onImageClicked,
                onAddImageClicked = onAddImageClicked,
            )
            ProductDetailImageUiState.Hidden -> Unit
        }
    }
}

@Composable
private fun ProductDetailImageGallery(
    items: List<ProductDetailImageUiItem>,
    listState: LazyListState,
    onImageClicked: () -> Unit,
    onAddImageClicked: () -> Unit,
) {
    val itemSpacing = dimensionResource(R.dimen.major_100)
    val edgePadding = dimensionResource(R.dimen.minor_100)
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = edgePadding),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        modifier = Modifier
            .height(dimensionResource(R.dimen.image_major_120))
            .testTag(ProductDetailTestTags.IMAGE_GALLERY),
    ) {
        items(
            items = items,
            key = ProductDetailImageUiItem::key,
            contentType = { it::class },
        ) { item ->
            when (item) {
                is ProductDetailImageUiItem.Persisted -> ProductDetailPersistedImage(
                    item = item,
                    onClick = onImageClicked,
                )
                is ProductDetailImageUiItem.Uploading -> ProductDetailUploadingImage(item = item)
                ProductDetailImageUiItem.Add -> ProductDetailAddImageTile(onClick = onAddImageClicked)
            }
        }
    }
}

@Composable
private fun ProductDetailPersistedImage(
    item: ProductDetailImageUiItem.Persisted,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small))
    val imageDescription = stringResource(R.string.product_image_content_description)
    Box(
        modifier = Modifier
            .height(dimensionResource(R.dimen.image_major_120))
            .clip(shape)
            .border(
                width = dimensionResource(R.dimen.minor_10),
                color = colorResource(R.color.image_border_color),
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = imageDescription },
    ) {
        ProductDetailGalleryImage(
            item = item,
            modifier = Modifier.height(dimensionResource(R.dimen.image_major_120)),
        )
        if (item.isCover) {
            Box(modifier = Modifier.matchParentSize()) {
                ProductDetailCoverBadge(modifier = Modifier.align(Alignment.TopStart))
            }
        }
    }
}

@Composable
private fun ProductDetailUploadingImage(item: ProductDetailImageUiItem.Uploading) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small))
    val imageDescription = stringResource(R.string.product_image_content_description)
    Box(
        modifier = Modifier
            .height(dimensionResource(R.dimen.image_major_120))
            .semantics { contentDescription = imageDescription },
    ) {
        Box(
            modifier = Modifier
                .alpha(PRODUCT_DETAIL_UPLOADING_IMAGE_ALPHA)
                .clip(shape)
                .border(
                    width = dimensionResource(R.dimen.minor_10),
                    color = colorResource(R.color.image_border_color),
                    shape = shape,
                ),
        ) {
            ProductDetailGalleryImage(
                item = item,
                modifier = Modifier.height(dimensionResource(R.dimen.image_major_120)),
            )
        }
        WooCircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .size(dimensionResource(R.dimen.major_300)),
        )
    }
}

@Composable
private fun ProductDetailAddImageTile(onClick: () -> Unit) {
    val shape = RoundedCornerShape(dimensionResource(R.dimen.corner_radius_small))
    val description = stringResource(R.string.product_add_image_content_description)
    Box(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.minor_100))) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(dimensionResource(R.dimen.image_major_120))
                .clip(shape)
                .border(
                    width = dimensionResource(R.dimen.minor_10),
                    color = colorResource(R.color.image_border_color),
                    shape = shape,
                )
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gridicons_add_image),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(dimensionResource(R.dimen.major_150)),
            )
        }
    }
}

@Composable
private fun ProductDetailGalleryImage(
    item: ProductDetailImageUiItem,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var lastKnownAspectRatio by rememberSaveable(item.key) {
        mutableFloatStateOf(DEFAULT_IMAGE_ASPECT_RATIO)
    }
    val heightPx = with(LocalDensity.current) {
        dimensionResource(R.dimen.image_major_120).roundToPx()
    }
    val request = when (item) {
        is ProductDetailImageUiItem.Persisted -> {
            rememberPhotonImageRequest(
                originalUrl = item.source,
                height = heightPx,
                quality = PhotonUtils.Quality.LOW,
            ) {
                crossfade(true)
                placeholder(R.drawable.product_detail_image_background)
                error(R.drawable.ic_product)
                size(Dimension.Undefined, Dimension.Pixels(heightPx))
            }
        }
        is ProductDetailImageUiItem.Uploading -> {
            remember(context, item.source, heightPx) {
                ImageRequest.Builder(context)
                    .data(item.source)
                    .crossfade(true)
                    .placeholder(R.drawable.product_detail_image_background)
                    .error(R.drawable.ic_product)
                    .size(Dimension.Undefined, Dimension.Pixels(heightPx))
                    .build()
            }
        }
        ProductDetailImageUiItem.Add -> return
    }

    val painter = rememberAsyncImagePainter(
        model = request,
        imageLoader = context.imageLoader,
        contentScale = ContentScale.Fit,
    )
    val successAspectRatio = painter.state.successAspectRatio()
    LaunchedEffect(successAspectRatio) {
        successAspectRatio?.let { lastKnownAspectRatio = it }
    }
    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .aspectRatio(successAspectRatio ?: lastKnownAspectRatio),
    )
}

private fun AsyncImagePainter.State.successAspectRatio(): Float? {
    val size = (this as? AsyncImagePainter.State.Success)?.painter?.intrinsicSize
        ?: return null
    if (!size.width.isFinite() || size.width <= 0F) return null
    if (!size.height.isFinite() || size.height <= 0F) return null
    return size.width / size.height
}

@Composable
private fun ProductDetailCoverBadge(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.product_cover_photo_tag)
    Text(
        text = description,
        color = WooTheme.colors.onPrimary,
        style = WooTheme.text.labelSmall.strong,
        modifier = modifier
            .padding(
                start = dimensionResource(R.dimen.minor_50),
                top = dimensionResource(R.dimen.minor_50),
            )
            .clip(RoundedCornerShape(dimensionResource(R.dimen.minor_75)))
            .background(WooTheme.colors.primary)
            .padding(
                horizontal = dimensionResource(R.dimen.minor_75),
                vertical = dimensionResource(R.dimen.minor_50),
            )
            .clearAndSetSemantics { contentDescription = description },
    )
}

@Composable
private fun ProductDetailAddImage(onClick: () -> Unit) {
    val description = stringResource(R.string.product_add_image_content_description)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.image_major_120))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(WooTheme.padding.padding5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_gridicons_add_image),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(ADD_IMAGE_ICON_SIZE),
        )
        Text(
            text = stringResource(R.string.product_image_add),
            color = WooTheme.colors.surface.onDefault,
            style = WooTheme.text.bodyMedium.emphasized,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProductDetailImagesUnavailable(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.image_major_120))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(WooTheme.padding.padding4),
        contentAlignment = Alignment.Center,
    ) {
        WooNoticeBanner(
            title = AnnotatedString.fromHtml(stringResource(R.string.images_unavailable_notice)).text,
            tone = WooNoticeBannerTone.Warning,
            leadingIcon = {
                Icon(
                    imageVector = WooIcons.Regular.CircleInfo,
                    contentDescription = null,
                )
            },
        )
    }
}

@Composable
private fun ProductDetailImageLoading() {
    val inspectionMode = LocalInspectionMode.current
    var isVisible by remember { mutableStateOf(inspectionMode) }
    LaunchedEffect(inspectionMode) {
        if (!inspectionMode) {
            delay(LOADING_DELAY_MS)
            isVisible = true
        }
    }
    if (isVisible) {
        SkeletonView(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.image_major_120))
                .padding(WooTheme.padding.padding5)
                .clip(RoundedCornerShape(WooTheme.radius.medium)),
        )
    }
}

@Composable
fun ProductDetailUploadError(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        Surface(
            color = WooTheme.colors.surface.bright,
            modifier = modifier.fillMaxWidth(),
        ) {
            WooOutlinedButton(
                text = stringResource(R.string.product_open_upload_screen),
                onClick = onClick,
                modifier = Modifier.padding(
                    horizontal = WooTheme.padding.padding7,
                    vertical = WooTheme.padding.padding3,
                ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailGalleryPreview() {
    ProductDetailImagePreview(state = previewGalleryState())
}

@PreviewLightDark
@Composable
private fun ProductDetailUploadingPreview() {
    ProductDetailImagePreview(
        state = ProductDetailImageUiState.Gallery(
            listOf(
                ProductDetailImageUiItem.Uploading("uploading_preview", previewImageSource()),
                ProductDetailImageUiItem.Persisted("persisted_preview", previewImageSource(), true),
                ProductDetailImageUiItem.Add,
            )
        )
    )
}

@PreviewLightDark
@Composable
private fun ProductDetailAddImagePreview() {
    ProductDetailImagePreview(state = ProductDetailImageUiState.AddImage)
}

@PreviewLightDark
@Composable
private fun ProductDetailImageUnavailablePreview() {
    ProductDetailImagePreview(state = ProductDetailImageUiState.Unavailable)
}

@PreviewLightDark
@Composable
private fun ProductDetailImageLoadingPreview() {
    ProductDetailImagePreview(state = ProductDetailImageUiState.Loading)
}

@PreviewLightDark
@Composable
private fun ProductDetailUploadErrorPreview() {
    WooDesignSystemThemeWithBackground {
        Column(modifier = Modifier.width(PREVIEW_WIDTH)) {
            ProductDetailImageHeader(
                state = previewGalleryState(),
                onImageClicked = {},
                onAddImageClicked = {},
                onImagesUnavailableClicked = {},
            )
            ProductDetailUploadError(isVisible = true, onClick = {})
        }
    }
}

@Preview(name = "Large font", fontScale = 2f, widthDp = 360)
@Composable
private fun ProductDetailAddImageLargeFontPreview() {
    ProductDetailImagePreview(state = ProductDetailImageUiState.AddImage)
}

@Preview(name = "RTL gallery", locale = "ar", widthDp = 360)
@Composable
private fun ProductDetailGalleryRtlPreview() {
    ProductDetailImagePreview(state = previewGalleryState())
}

@Composable
private fun ProductDetailImagePreview(state: ProductDetailImageUiState) {
    WooDesignSystemThemeWithBackground {
        ProductDetailImageHeader(
            state = state,
            onImageClicked = {},
            onAddImageClicked = {},
            onImagesUnavailableClicked = {},
            modifier = Modifier.width(PREVIEW_WIDTH),
        )
    }
}

@Composable
private fun previewGalleryState() = ProductDetailImageUiState.Gallery(
    listOf(
        ProductDetailImageUiItem.Persisted("persisted_cover", previewImageSource(), true),
        ProductDetailImageUiItem.Persisted("persisted_second", previewImageSource(), false),
        ProductDetailImageUiItem.Add,
    )
)

@Composable
private fun previewImageSource(): String {
    val context = LocalContext.current
    return "android.resource://${context.packageName}/${R.drawable.blaze_campaign_product_placeholder}"
}

private val ADD_IMAGE_ICON_SIZE = 40.dp
private val PREVIEW_WIDTH = 360.dp
private const val PRODUCT_DETAIL_UPLOADING_IMAGE_ALPHA = 0.5F
private const val DEFAULT_IMAGE_ASPECT_RATIO = 1F
private const val LOADING_DELAY_MS = 250L
