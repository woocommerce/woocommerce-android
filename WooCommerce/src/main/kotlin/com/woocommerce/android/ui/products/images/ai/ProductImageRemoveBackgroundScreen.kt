@file:Suppress("MagicNumber")

package com.woocommerce.android.ui.products.images.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BottomActionMenu(
    modifier: Modifier = Modifier,
    onCancelClicked: () -> Unit,
    onSaveClicked: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(dimensionResource(R.dimen.major_100)),
        color = MaterialTheme.colors.surface,
        elevation = 8.dp,
        shape = RoundedCornerShape(dimensionResource(R.dimen.minor_100))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.major_100)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancelClicked) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = MaterialTheme.typography.button
                )
            }

            Button(onClick = onSaveClicked) {
                Text(
                    text = stringResource(R.string.save_copy),
                    style = MaterialTheme.typography.button
                )
            }
        }
    }
}

@Composable
fun ProductImageRemoveBackgroundScreen(
    state: State<ViewState>,
    onBackPressed: () -> Unit,
    onCancelClicked: () -> Unit,
    onSaveClicked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.remove_background_title)) },
                backgroundColor = MaterialTheme.colors.surface,
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val viewState = state.value) {
            is ViewState.Success -> {
                Box(
                    modifier = Modifier.padding(paddingValues)
                ) {
                    AsyncImage(
                        model = viewState.bitmap,
                        contentDescription = stringResource(R.string.product_image_content_description),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    BottomActionMenu(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onCancelClicked = onCancelClicked,
                        onSaveClicked = onSaveClicked
                    )
                }
            }

            is ViewState.BackgroundProcessingInProgress -> {
                Box(modifier = Modifier.padding(paddingValues)) {
                    val isImageLoaded = remember { mutableStateOf(false) }
                    AsyncImage(
                        model = viewState.imageUri,
                        contentDescription = stringResource(R.string.product_image_content_description),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                        onSuccess = {
                            isImageLoaded.value = true
                        }
                    )
                    if (isImageLoaded.value) {
                        MagicSparkles()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ProductImageRemoveBackgroundScreenPreview() {
    WooThemeWithBackground {
        val state = remember {
            mutableStateOf(ViewState.Success(createBitmap(23, 23)))
        }
        ProductImageRemoveBackgroundScreen(
            state = state,
            onBackPressed = { },
            onCancelClicked = { },
            onSaveClicked = { }
        )
    }
}

@Preview
@Composable
fun ProductImageRemoveBackgroundScreenProgressPreview() {
    WooThemeWithBackground {
        val state = remember {
            mutableStateOf(ViewState.BackgroundProcessingInProgress("".toUri()))
        }
        ProductImageRemoveBackgroundScreen(
            state = state,
            onBackPressed = { },
            onCancelClicked = { },
            onSaveClicked = { }
        )
    }
}
