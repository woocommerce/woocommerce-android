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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
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
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState.CarouselItem
import okhttp3.OkHttpClient

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
                    .verticalScroll(rememberScrollState())
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
            text = stringResource(id = string.theme_picker_title),
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = dimen.major_100),
                    end = dimensionResource(id = dimen.major_100),
                )
        )
        Text(
            text = stringResource(id = string.theme_picker_description),
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
                    is CarouselItem.Theme -> Theme(item.name, item.screenshotUrl)
                    is CarouselItem.Message -> Message(item.title, item.description, Modifier.width(320.dp))
                }
            }
        }
    }
}

@Composable
private fun Message(title: String, description: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
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
private fun Theme(name: String, screenshotUrl: String) {
    val themeModifier = Modifier.width(240.dp)
    Card(
        shape = RoundedCornerShape(dimensionResource(id = dimen.minor_100)),
        elevation = dimensionResource(id = dimen.minor_50),
        modifier = themeModifier
    ) {
        val imageLoader = ImageLoader.Builder(LocalContext.current)
            .okHttpClient {
                OkHttpClient.Builder()
                    .followRedirects(false)
                    .build()
            }
            .build()

        val request = ImageRequest.Builder(LocalContext.current)
            .data(screenshotUrl)
            .crossfade(true)
            .build()

        SubcomposeAsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = stringResource(string.settings_app_theme_title),
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.fillMaxHeight()
        ) {
            when (painter.state) {
                is AsyncImagePainter.State.Error -> {
                    Message(
                        title = stringResource(id = string.theme_picker_carousel_placeholder_title, name),
                        description = stringResource(id = string.theme_picker_carousel_placeholder_message),
                        themeModifier
                    )
                }
                else -> {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewThemePickerError() {
    ThemePicker(ViewState.Error, Modifier)
}

@Preview
@Composable
private fun PreviewThemePickerLoading() {
    ThemePicker(ViewState.Loading, Modifier)
}
