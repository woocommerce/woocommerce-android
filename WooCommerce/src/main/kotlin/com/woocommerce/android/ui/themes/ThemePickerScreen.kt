package com.woocommerce.android.ui.themes

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.util.DebugLogger
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.themes.ThemePickerViewModel.CarouselState
import com.woocommerce.android.ui.themes.ThemePickerViewModel.CarouselState.Success.CarouselItem
import com.woocommerce.android.ui.themes.ThemePickerViewModel.CurrentThemeState
import okhttp3.OkHttpClient

@Composable
fun ThemePickerScreen(viewModel: ThemePickerViewModel) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        Scaffold(topBar = {
            Toolbar(
                title = {
                    Text(
                        if (viewState.isFromStoreCreation) ""
                        else stringResource(id = R.string.settings_themes)
                    )
                },
                navigationIcon = Filled.ArrowBack,
                onNavigationButtonClick = viewModel::onArrowBackPressed,
                actions = {
                    if (viewState.isFromStoreCreation) {
                        TextButton(onClick = viewModel::onSkipPressed) {
                            Text(text = stringResource(id = R.string.skip))
                        }
                    }
                }
            )
        }) { padding ->
            ThemePicker(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(MaterialTheme.colors.surface),
                viewState = viewState,
                onThemeTapped = viewModel::onThemeTapped,
                onThemeScreenshotFailure = viewModel::onThemeScreenshotFailure
            )
        }
    }
}

@Composable
private fun ThemePicker(
    modifier: Modifier,
    viewState: ThemePickerViewModel.ViewState,
    onThemeTapped: (CarouselItem.Theme) -> Unit,
    onThemeScreenshotFailure: (String, Throwable) -> Unit
) {
    Column(
        modifier = modifier
            .padding(vertical = dimensionResource(id = R.dimen.major_100))
            .fillMaxSize()
    ) {
        CurrentTheme(viewState.currentThemeState)
        Header(
            isFromStoreCreation = viewState.isFromStoreCreation,
            modifier = Modifier.fillMaxWidth()
        )
        Carousel(viewState.carouselState, onThemeTapped, onThemeScreenshotFailure)
    }
}

@Composable
private fun CurrentTheme(
    currentThemeState: CurrentThemeState,
    modifier: Modifier = Modifier
) {
    if (currentThemeState == CurrentThemeState.Hidden) {
        return
    }

    Column(modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))) {
        Text(
            text = stringResource(id = R.string.theme_picker_current_theme_title),
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        when (currentThemeState) {
            is CurrentThemeState.Loading -> {
                SkeletonView(
                    width = dimensionResource(id = R.dimen.skeleton_text_medium_width),
                    height = dimensionResource(id = R.dimen.skeleton_text_height_100)
                )
            }

            is CurrentThemeState.Success -> {
                Text(
                    text = currentThemeState.themeName,
                    style = MaterialTheme.typography.body1,
                    color = colorResource(id = color.color_on_surface_medium)
                )
            }

            else -> {}
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
    }
}

@Composable
private fun Header(
    isFromStoreCreation: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        if (isFromStoreCreation) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            Text(
                text = stringResource(id = R.string.theme_picker_title),
                style = MaterialTheme.typography.h5,
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )
            Text(
                text = stringResource(id = R.string.theme_picker_description),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = color.color_on_surface_medium),
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_250)))
        } else {
            Text(
                text = stringResource(id = R.string.theme_picker_settings_title),
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }
}

@Composable
private fun ColumnScope.Carousel(
    state: CarouselState,
    onThemeTapped: (CarouselItem.Theme) -> Unit,
    onThemeScreenshotFailure: (String, Throwable) -> Unit
) {
    when (state) {
        is CarouselState.Loading -> {
            Loading()
        }

        is CarouselState.Error -> {
            Error()
        }

        is CarouselState.Success -> {
            Carousel(state.carouselItems, onThemeTapped, onThemeScreenshotFailure)
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
        title = stringResource(id = R.string.theme_picker_error_title),
        description = annotatedStringRes(stringResId = R.string.theme_picker_error_message),
        color = color.color_error
    )
}

@Composable
private fun Carousel(
    items: List<CarouselItem>,
    onThemeTapped: (CarouselItem.Theme) -> Unit,
    onThemeScreenshotFailure: (String, Throwable) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .height(480.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        contentPadding = PaddingValues(start = dimensionResource(id = R.dimen.major_100))
    ) {
        items(items) { item ->
            when (item) {
                is CarouselItem.Theme -> Theme(item, onThemeTapped, onThemeScreenshotFailure)
                is CarouselItem.Message -> Message(
                    title = item.title,
                    description = AnnotatedString(item.description),
                    modifier = Modifier.width(320.dp)
                )
            }
        }
    }
}

@Composable
private fun Message(
    title: String,
    description: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Int = R.color.color_on_surface_medium
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(dimensionResource(id = R.dimen.major_100)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = color),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(bottom = dimensionResource(id = R.dimen.major_100))
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
private fun Theme(
    theme: CarouselItem.Theme,
    onThemeTapped: (CarouselItem.Theme) -> Unit,
    onScreenShotFailed: (String, Throwable) -> Unit
) {
    val themeModifier = Modifier.width(240.dp)
    Card(
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100)),
        elevation = dimensionResource(id = R.dimen.minor_50),
        modifier = themeModifier.clickable { onThemeTapped(theme) }
    ) {
        val imageLoader = ImageLoader.Builder(LocalContext.current)
            .okHttpClient {
                OkHttpClient.Builder()
                    .followRedirects(false)
                    .build()
            }
            .logger(if (BuildConfig.DEBUG) DebugLogger() else null)
            .build()

        val request = ImageRequest.Builder(LocalContext.current)
            .data(theme.screenshotUrl)
            .crossfade(true)
            .listener(
                onError = { _, result ->
                    onScreenShotFailed(theme.name, result.throwable)
                }
            )
            .build()

        SubcomposeAsyncImage(
            model = request,
            imageLoader = imageLoader,
            contentDescription = stringResource(R.string.settings_app_theme_title),
            error = {
                val errorMessage = buildAnnotatedString {
                    val ctaText = stringResource(id = R.string.theme_picker_carousel_error_placeholder_message_cta)
                    val message = stringResource(id = R.string.theme_picker_carousel_error_placeholder_message, ctaText)
                    append(message)
                    addStyle(
                        SpanStyle(color = MaterialTheme.colors.primary),
                        message.indexOf(ctaText),
                        message.indexOf(ctaText) + ctaText.length
                    )
                }

                Message(
                    title = theme.name,
                    description = errorMessage,
                    modifier = themeModifier
                )
            },
            contentScale = ContentScale.FillHeight,
            modifier = Modifier.fillMaxHeight()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewThemePickerError() {
    WooThemeWithBackground {
        ThemePicker(
            modifier = Modifier,
            viewState = ThemePickerViewModel.ViewState(
                isFromStoreCreation = true,
                carouselState = CarouselState.Error,
                currentThemeState = CurrentThemeState.Hidden
            ),
            onThemeTapped = {},
            onThemeScreenshotFailure = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewThemePickerLoading() {
    WooThemeWithBackground {
        ThemePicker(
            modifier = Modifier,
            viewState = ThemePickerViewModel.ViewState(
                isFromStoreCreation = true,
                carouselState = CarouselState.Error,
                currentThemeState = CurrentThemeState.Hidden
            ),
            onThemeTapped = {},
            onThemeScreenshotFailure = { _, _ -> }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewThemePickerStoreCreation() {
    WooThemeWithBackground {
        ThemePicker(
            modifier = Modifier,
            viewState = ThemePickerViewModel.ViewState(
                isFromStoreCreation = true,
                carouselState = CarouselState.Success(
                    carouselItems = listOf(
                        CarouselItem.Theme(themeId = "tsubaki", name = "Tsubaki", screenshotUrl = ""),
                        CarouselItem.Theme(themeId = "tsubaki", name = "Tsubaki", screenshotUrl = ""),
                        CarouselItem.Theme(themeId = "tsubaki", name = "Tsubaki", screenshotUrl = "")
                    )
                ),
                currentThemeState = CurrentThemeState.Hidden
            ),
            onThemeTapped = {},
            onThemeScreenshotFailure = { _, _ -> }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewThemePickerSettings() {
    WooThemeWithBackground {
        ThemePicker(
            modifier = Modifier,
            viewState = ThemePickerViewModel.ViewState(
                isFromStoreCreation = false,
                carouselState = CarouselState.Success(
                    carouselItems = listOf(
                        CarouselItem.Theme(themeId = "tsubaki", name = "Tsubaki", screenshotUrl = ""),
                        CarouselItem.Theme(themeId = "tsubaki", name = "Tsubaki", screenshotUrl = ""),
                        CarouselItem.Theme(themeId = "tsubaki", name = "Tsubaki", screenshotUrl = "")
                    )
                ),
                currentThemeState = CurrentThemeState.Success(themeName = "Tsubaki")
            ),
            onThemeTapped = {},
            onThemeScreenshotFailure = { _, _ -> }
        )
    }
}
