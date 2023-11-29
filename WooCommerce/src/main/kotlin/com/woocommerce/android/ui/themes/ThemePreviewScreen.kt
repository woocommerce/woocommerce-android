package com.woocommerce.android.ui.themes

import androidx.activity.result.ActivityResultRegistry
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue.HalfExpanded
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState
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
    onPageSelected: (String) -> Unit,
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
                onPageSelected = {
                    coroutineScope.launch { modalSheetState.hide() }
                    onPageSelected(it)
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                Toolbar(
                    title = stringResource(id = string.theme_preview_title),
                    navigationIcon = Filled.ArrowBack,
                    onNavigationButtonClick = onBackNavigationClicked,
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

@Composable
@Suppress("UNUSED_PARAMETER")
private fun ThemeDemoPagesBottomSheet(
    onPageSelected: (String) -> Unit,
) {
    // TODO display demo pager here
}
