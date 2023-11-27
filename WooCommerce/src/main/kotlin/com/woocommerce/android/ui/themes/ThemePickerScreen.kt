package com.woocommerce.android.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy.DISABLED
import coil.request.ImageRequest
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState.CarouselItem

@Composable
fun ThemePickerScreen(viewModel: ThemePickerViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        Scaffold(topBar = {
            Toolbar(
                title = { Text("") },
                navigationIcon = Filled.ArrowBack,
                onNavigationButtonClick = viewModel::onArrowBackPressed,
                actions = {
                    TextButton(onClick = viewModel::onSkipPressed) {
                        Text(text = stringResource(id = string.skip))
                    }
                }
            )
        }) { padding ->
            ThemePickerScreenCarousel(
                modifier = Modifier
                    .padding(padding)
                    .background(MaterialTheme.colors.surface),
                viewState.carouselItems
            )
        }
    }
}

@Composable
private fun ThemePickerScreenCarousel(
    modifier: Modifier,
    items: List<CarouselItem>
) {
    Column(
        modifier = modifier
            .padding(vertical = dimensionResource(id = dimen.major_100))
            .fillMaxSize()
    ) {
        Text(
            text = stringResource(id = string.store_creation_theme_picker_title),
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = dimen.major_100),
                    end = dimensionResource(id = dimen.major_100),
                )
        )
        Text(
            text = stringResource(id = string.store_creation_theme_picker_description),
            style = MaterialTheme.typography.subtitle1,
            color = colorResource(id = color.color_on_surface_medium),
            modifier = Modifier.padding(dimensionResource(id = dimen.major_100))
        )

        LazyRow(
            modifier = Modifier
                .padding(top = dimensionResource(id = dimen.major_150))
                .height(480.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_100)),
            contentPadding = PaddingValues(start = dimensionResource(id = dimen.major_100))
        ) {
            items(items) { item ->
                when (item) {
                    is CarouselItem.Theme -> Theme(item.screenshotUrl)
                    is CarouselItem.Message -> Message(item.title, item.description)
                }
            }
        }
    }
}

@Composable
private fun Message(title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .width(320.dp)
            .padding(dimensionResource(id = dimen.major_100)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = color.color_on_surface_medium),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = dimensionResource(id = dimen.major_100))
                    .fillMaxWidth()
            )
            Text(
                text = description,
                color = colorResource(id = color.color_on_surface_medium),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Theme(screenshotUrl: String) {
    Card(
        shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100)),
        elevation = dimensionResource(id = dimen.minor_50),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(screenshotUrl)
                .diskCachePolicy(DISABLED)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(string.settings_app_theme_title),
            contentScale = ContentScale.FillHeight,
            modifier = Modifier
                .fillMaxHeight()
        )
    }
}
