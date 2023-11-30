package com.woocommerce.android.ui.themes

import androidx.activity.result.ActivityResultRegistry
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue.HalfExpanded
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ThemeDemoPage
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun ThemePreviewScreen(
    viewModel: ThemePreviewViewModel,
    userAgent: UserAgent,
    wpcomWebViewAuthenticator: WPComWebViewAuthenticator,
    activityRegistry: ActivityResultRegistry,
) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        ThemePreviewScreen(
            state = viewState,
            userAgent = userAgent,
            wpComWebViewAuthenticator = wpcomWebViewAuthenticator,
            activityRegistry = activityRegistry,
            viewModel::onPageSelected,
            viewModel::onBackNavigationClicked,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ThemePreviewScreen(
    state: ViewState,
    userAgent: UserAgent,
    wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    activityRegistry: ActivityResultRegistry,
    onPageSelected: (ThemeDemoPage) -> Unit,
    onBackNavigationClicked: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = Hidden,
        confirmValueChange = { it != HalfExpanded }
    )

    ModalBottomSheetLayout(
        sheetState = modalSheetState,
        sheetShape = RoundedCornerShape(
            topStart = dimensionResource(id = dimen.minor_100),
            topEnd = dimensionResource(id = dimen.minor_100)
        ),
        scrimColor =
        // Overriding scrim color for dark theme because of the following bug affecting ModalBottomSheetLayout:
        // https://issuetracker.google.com/issues/183697056
        if (isSystemInDarkTheme()) colorResource(id = color.color_scrim_background)
        else ModalBottomSheetDefaults.scrimColor,
        sheetContent = {
            ThemeDemoPagesBottomSheet(
                pages = state.themePages,
                onPageSelected = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onPageSelected(it)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) {
        Scaffold(
            topBar = {
                CustomToolbar(
                    state,
                    coroutineScope,
                    modalSheetState,
                    onBackNavigationClicked
                )
            },
            backgroundColor = MaterialTheme.colors.surface
        ) { paddingValues ->
            WCWebView(
                url = state.demoUri,
                userAgent = userAgent,
                wpComAuthenticator = wpComWebViewAuthenticator,
                activityRegistry = activityRegistry,
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CustomToolbar(
    state: ViewState,
    coroutineScope: CoroutineScope,
    modalSheetState: ModalBottomSheetState,
    onBackNavigationClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(drawable.ic_gridicons_cross_24dp),
            contentDescription = "",
            modifier = Modifier
                .clickable { onBackNavigationClicked() }
                .padding(dimensionResource(id = R.dimen.major_100))
        )
        Column(
            modifier = Modifier
                .padding(start = dimensionResource(id = R.dimen.major_150))
                .clickable {
                    if (state.themePages.isNotEmpty()) {
                        coroutineScope.launch {
                            if (modalSheetState.isVisible)
                                modalSheetState.hide()
                            else {
                                modalSheetState.show()
                            }
                        }
                    }
                }
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            Text(
                text = stringResource(id = string.theme_preview_title),
                style = MaterialTheme.typography.body1,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.themePages.firstOrNull { it.isLoaded }?.title
                        ?: stringResource(id = R.string.theme_preview_default_page_loaded),
                    style = MaterialTheme.typography.caption,
                )
                Icon(
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.major_100))
                        .padding(start = dimensionResource(id = R.dimen.minor_50)),
                    painter = painterResource(drawable.ic_arrow_down),
                    contentDescription = "",
                    tint = colorResource(id = color.color_on_surface)
                )
            }
        }
    }
}

@Composable
private fun ThemeDemoPagesBottomSheet(
    pages: List<ThemeDemoPage>,
    onPageSelected: (ThemeDemoPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        Text(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    top = dimensionResource(id = R.dimen.minor_100)
                ),
            text = stringResource(id = R.string.theme_preview_bottom_sheet_pages_title),
            style = MaterialTheme.typography.h6,
        )
        Text(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = R.dimen.major_100),
                    bottom = dimensionResource(id = R.dimen.minor_100)
                ),
            text = stringResource(id = R.string.theme_preview_bottom_sheet_pages_subtitle),
            style = MaterialTheme.typography.subtitle2,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        Divider()
        pages.forEachIndexed { index, page ->
            Text(
                text = page.title,
                style = MaterialTheme.typography.body1,
                modifier = Modifier
                    .clickable { onPageSelected(page) }
                    .fillMaxWidth()
                    .padding(
                        top = dimensionResource(id = R.dimen.major_85),
                        start = dimensionResource(id = R.dimen.major_100),
                        end = dimensionResource(id = R.dimen.major_100),
                        bottom = dimensionResource(id = R.dimen.major_85)
                    )
            )
            if (index == pages.lastIndex) {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))
            }
        }
    }
}
