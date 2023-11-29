package com.woocommerce.android.ui.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import coil.util.DebugLogger
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState
import com.woocommerce.android.ui.themes.ThemePickerViewModel.ViewState.Success.CarouselItem
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
            ThemePicker(
                viewState = viewState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colors.surface)
            )
        }
    }
}

@Composable
private fun ThemePicker(
    viewState: ViewState,
    modifier: Modifier
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

        when (viewState) {
            is ViewState.Loading -> {
                Loading()
            }

            is ViewState.Error -> {
                Error()
            }

            is ViewState.Success -> {
                Carousel(viewState.carouselItems)
            }
        }
    }
}

@Composable
private fun ColumnScope.Loading() {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
            .weight(1f)
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun ColumnScope.Error() {
    Message(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        title = stringResource(id = string.theme_picker_error_title),
        description = stringResource(id = string.theme_picker_error_message),
        color = color.color_error
    )
}

@Composable
private fun Carousel(items: List<CarouselItem>) {
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
                is CarouselItem.Message -> Message(modifier = Modifier.width(320.dp), item.title, item.description)
            }
        }
    }
}

@Composable
private fun Message(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    color: Int = R.color.color_on_surface_medium
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(dimensionResource(id = dimen.major_100)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = color),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = dimensionResource(id = dimen.major_100))
                    .fillMaxWidth()
            )
            Text(
                text = description,
                color = colorResource(id = color),
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
            .logger(DebugLogger())
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
                        modifier = themeModifier,
                        title = stringResource(id = string.theme_picker_carousel_placeholder_title, name),
                        description = stringResource(id = string.theme_picker_carousel_placeholder_message)
                    )
                }
                else -> {
                    SubcomposeAsyncImageContent()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewThemePickerError() {
    WooThemeWithBackground {
        ThemePicker(ViewState.Error, Modifier)
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewThemePickerLoading() {
    WooThemeWithBackground {
        ThemePicker(ViewState.Loading, Modifier)
    }
}
