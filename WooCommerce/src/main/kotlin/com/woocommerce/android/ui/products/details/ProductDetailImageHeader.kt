package com.woocommerce.android.ui.products.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.component.WooOutlinedButton
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.designsystem.icons.CircleInfo
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import kotlinx.coroutines.delay

@Composable
fun ProductDetailImageHeader(
    state: ProductDetailImageUiState,
    onAddImageClicked: () -> Unit,
    onImagesUnavailableClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = WooTheme.colors.surface.bright,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensionResource(R.dimen.image_major_120)),
    ) {
        when (state) {
            ProductDetailImageUiState.Loading -> ProductDetailImageLoading()
            ProductDetailImageUiState.AddImage -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimensionResource(R.dimen.image_major_120))
                    .clickable(onClick = onAddImageClicked)
                    .padding(WooTheme.padding.padding5),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_gridicons_add_image),
                    contentDescription = null,
                    tint = WooTheme.colors.primary,
                    modifier = Modifier.size(ADD_IMAGE_ICON_SIZE),
                )
                Text(
                    text = stringResource(R.string.product_image_add),
                    color = WooTheme.colors.surface.onDefault,
                    style = WooTheme.text.bodyMedium.emphasized,
                    textAlign = TextAlign.Center,
                )
            }
            ProductDetailImageUiState.Unavailable -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimensionResource(R.dimen.image_major_120))
                    .clickable(onClick = onImagesUnavailableClicked)
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
            ProductDetailImageUiState.Gallery,
            ProductDetailImageUiState.Hidden -> Unit
        }
    }
}

@Composable
private fun ProductDetailImageLoading() {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADING_DELAY_MS)
        isVisible = true
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
                modifier = Modifier
                    .padding(
                        horizontal = WooTheme.padding.padding7,
                        vertical = WooTheme.padding.padding3,
                    ),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailAddImagePreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailImageHeader(
            state = ProductDetailImageUiState.AddImage,
            onAddImageClicked = {},
            onImagesUnavailableClicked = {},
            modifier = Modifier.width(360.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailImageUnavailablePreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailImageHeader(
            state = ProductDetailImageUiState.Unavailable,
            onAddImageClicked = {},
            onImagesUnavailableClicked = {},
            modifier = Modifier.width(360.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailImageLoadingPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailImageHeader(
            state = ProductDetailImageUiState.Loading,
            onAddImageClicked = {},
            onImagesUnavailableClicked = {},
            modifier = Modifier.size(width = 360.dp, height = 120.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailUploadErrorPreview() {
    WooDesignSystemThemeWithBackground {
        ProductDetailUploadError(isVisible = true, onClick = {})
    }
}

private val ADD_IMAGE_ICON_SIZE = 40.dp
private const val LOADING_DELAY_MS = 250L
