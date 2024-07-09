package com.woocommerce.android.ui.products.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.woocommerce.android.model.Image
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun FullScreenImageViewer(
    image: Image,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Popup {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colors.background)
        ) {
            Toolbar(
                navigationIcon = Icons.Filled.Close,
                onNavigationButtonClick = onDismiss
            )
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(image.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Inside,
                alignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
