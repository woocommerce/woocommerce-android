package com.woocommerce.android.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.theme.WooTypography
import com.woocommerce.android.ui.media.MediaUploadErrorListViewModel.ErrorUiModel

@Composable
fun MediaUploadErrorListScreen(viewModel: MediaUploadErrorListViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        MediaUploadErrorListScreen(
            state = state,
            viewModel::onBackPressed,
            viewModel::onRetryUploadClicked
        )
    }
}

@Composable
private fun MediaUploadErrorListScreen(
    state: MediaUploadErrorListViewModel.ViewState,
    onBackPressed: () -> Unit,
    onRetryClicked: (ErrorUiModel) -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.product_upload_error_title),
                onNavigationButtonClick = onBackPressed,
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            )
        },
        content = { padding ->
            MediaUploadErrorList(
                errors = state.uploadErrorList,
                onRetryClicked = onRetryClicked,
                modifier = Modifier
                    .padding(padding)
                    .background(MaterialTheme.colors.surface)
                    .padding(horizontal = 16.dp)
            )
        }
    )
}

@Composable
private fun MediaUploadErrorList(
    errors: List<ErrorUiModel>,
    onRetryClicked: (ErrorUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(errors) { error ->
            MediaUploadErrorListItem(
                error = error,
                onRetryClicked = onRetryClicked,
                modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
fun MediaUploadErrorListItem(
    error: ErrorUiModel,
    onRetryClicked: (ErrorUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(error.localUri)
                .crossfade(true)
                .build(),
            fallback = painterResource(R.drawable.ic_product),
            placeholder = painterResource(R.drawable.ic_product),
            error = painterResource(R.drawable.ic_product),
            contentDescription = stringResource(id = R.string.product_image_content_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(48.dp)
                .height(48.dp)
                .clip(shape = RoundedCornerShape(size = 6.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                style = WooTypography.subtitle2,
                text = error.fileName
            )
            Text(
                style = WooTypography.body2,
                text = error.errorMessage
            )
        }
        IconButton(
            onClick = { onRetryClicked(error) },
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gridicons_refresh),
                contentDescription = stringResource(id = R.string.retry)
            )
        }
    }
}

@Preview
@Composable
fun MediaUploadErrorListItemPreview() {
    MediaUploadErrorListScreen(
        state = MediaUploadErrorListViewModel.ViewState(
            uploadErrorList = listOf(
                ErrorUiModel(
                    fileName = "image1345211331.jpg",
                    errorMessage = "Upload timeout",
                    localUri = ""
                ),
                ErrorUiModel(
                    fileName = "image987654.jpg",
                    errorMessage = "Upload failed, very long error message with multiple lines to check how" +
                        " the row behaves",
                    localUri = ""
                )
            )
        ),
        onBackPressed = {},
        onRetryClicked = {}
    )
}
