package com.woocommerce.android.ui.themes

import androidx.activity.result.ActivityResultRegistry
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue.HalfExpanded
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.Screen.ScreenType
import com.woocommerce.android.ui.compose.Screen.ScreenType.Desktop
import com.woocommerce.android.ui.compose.Screen.ScreenType.Mobile
import com.woocommerce.android.ui.compose.Screen.ScreenType.Tablet
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCWebView
import com.woocommerce.android.ui.compose.rememberScreen
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType.DESKTOP
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType.MOBILE
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType.TABLET
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun ThemePreviewScreen(
    viewModel: ThemePreviewViewModel,
    userAgent: UserAgent,
    wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    activityRegistry: ActivityResultRegistry,
) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        ThemePreviewScreen(
            state = viewState,
            userAgent = userAgent,
            wpComWebViewAuthenticator = wpComWebViewAuthenticator,
            activityRegistry = activityRegistry,
            viewModel::onPageSelected,
            viewModel::onBackNavigationClicked,
            viewModel::onPreviewTypeChanged
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
    onPreviewTypeChanged: (PreviewType) -> Unit
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
                    actions = {
                        ThemePreviewMenu(state.previewType, onPreviewTypeChanged)
                    }
                )
            },
            backgroundColor = MaterialTheme.colors.surface
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                val screen = rememberScreen()

                WCWebView(
                    url = state.demoUri,
                    userAgent = userAgent,
                    wpComAuthenticator = wpComWebViewAuthenticator,
                    initialScale = state.previewType.initialScale(screen.type),
                    loadWithOverviewMode = true,
                    activityRegistry = activityRegistry,
                    modifier = Modifier
                        .then(
                            if (state.previewType == MOBILE && screen.type != Mobile) {
                                Modifier.widthIn(max = Mobile.width.dp)
                            } else if (state.previewType == TABLET && screen.type == Desktop) {
                                Modifier.widthIn(max = Tablet.width.dp)
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        )
                        .weight(1f)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewMenu(
    selectedType: PreviewType,
    onPreviewTypeChanged: (PreviewType) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    @Composable
    fun PreviewMenuItem(
        previewType: PreviewType,
        @StringRes previewMenuTextResourceId: Int,
    ) {
        DropdownMenuItem(
            modifier = Modifier
                .height(dimensionResource(id = dimen.major_175)),
            onClick = {
                showMenu = false
                onPreviewTypeChanged(previewType)
            }
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = previewMenuTextResourceId))
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(dimensionResource(id = dimen.major_300))
                )
                if (selectedType == previewType) {
                    Icon(
                        imageVector = Outlined.Check,
                        tint = MaterialTheme.colors.primary,
                        contentDescription = stringResource(string.toggle_option_checked)
                    )
                }
            }
        }
    }

    IconButton(onClick = { showMenu = !showMenu }) {
        Icon(
            imageVector = Outlined.Devices,
            tint = MaterialTheme.colors.onSurface,
            contentDescription = stringResource(string.theme_preview_title),
        )
    }
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        PreviewMenuItem(MOBILE, string.theme_preview_type_mobile)
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
        PreviewMenuItem(TABLET, string.theme_preview_type_tablet)
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
        PreviewMenuItem(DESKTOP, string.theme_preview_type_desktop)
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun ThemeDemoPagesBottomSheet(
    onPageSelected: (String) -> Unit,
) {
    // TODO display demo pager here
}

@Composable
private fun PreviewType.initialScale(screenType: ScreenType): Int {
    return when (screenType) {
        Mobile -> {
            when (this) {
                MOBILE -> 0
                TABLET -> 160
                DESKTOP -> 110
            }
        }
        Tablet -> {
            when (this) {
                MOBILE -> 160
                TABLET -> 0
                DESKTOP -> 110
            }
        }
        Desktop -> {
            when (this) {
                MOBILE -> 160
                TABLET -> 0
                DESKTOP -> 0
            }
        }
    }
}
