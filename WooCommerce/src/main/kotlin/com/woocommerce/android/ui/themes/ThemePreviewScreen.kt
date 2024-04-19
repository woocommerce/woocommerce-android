package com.woocommerce.android.ui.themes

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue.HalfExpanded
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheetLayout
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ThemeDemoPage
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType.DESKTOP
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType.MOBILE
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType.TABLET
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.UserAgent

@Composable
fun ThemePreviewScreen(
    viewModel: ThemePreviewViewModel,
    userAgent: UserAgent,
    wpComWebViewAuthenticator: WPComWebViewAuthenticator
) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        ThemePreviewScreen(
            state = viewState,
            userAgent = userAgent,
            wpComWebViewAuthenticator = wpComWebViewAuthenticator,
            viewModel::onPageSelected,
            viewModel::onBackNavigationClicked,
            viewModel::onActivateThemeClicked,
            viewModel::onPreviewTypeChanged,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ThemePreviewScreen(
    state: ViewState,
    userAgent: UserAgent,
    wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    onPageSelected: (ThemeDemoPage) -> Unit,
    onBackNavigationClicked: () -> Unit,
    onActivateThemeClicked: () -> Unit,
    onPreviewTypeChanged: (PreviewType) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = Hidden,
        confirmValueChange = { it != HalfExpanded }
    )

    WCModalBottomSheetLayout(
        sheetState = modalSheetState,
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
                Toolbar(
                    title = {
                        DemoSectionsToolbar(
                            state,
                            coroutineScope,
                            modalSheetState
                        )
                    },
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationButtonClick = onBackNavigationClicked,
                    actions = {
                        ThemePreviewMenu(state.previewType, onPreviewTypeChanged)
                    }
                )
            },
            backgroundColor = MaterialTheme.colors.surface
        ) { paddingValues ->
            Column(
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
            ) {
                ThemePreviewWebView(
                    url = state.currentPageUri,
                    userAgent = userAgent,
                    wpComAuthenticator = wpComWebViewAuthenticator,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterHorizontally),
                    previewType = state.previewType
                )

                ThemePreviewBottomSection(
                    themeName = state.themeName,
                    isActivatingTheme = state.isActivatingTheme,
                    onActivateThemeClicked = onActivateThemeClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ThemePreviewBottomSection(
    themeName: String,
    isActivatingTheme: Boolean,
    onActivateThemeClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(id = dimen.major_100)
        ),
        modifier = modifier
    ) {
        Divider()
        WCColoredButton(
            onClick = onActivateThemeClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = dimen.major_100))
                .padding(bottom = dimensionResource(id = dimen.major_100))
        ) {
            if (isActivatingTheme) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size = dimensionResource(id = dimen.major_150)),
                    color = LocalContentColor.current,
                )
            } else {
                Text(
                    text = stringResource(id = R.string.theme_preview_activate_theme_button_settings, themeName)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun DemoSectionsToolbar(
    state: ViewState,
    coroutineScope: CoroutineScope,
    modalSheetState: ModalBottomSheetState,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .padding(start = dimensionResource(id = dimen.major_150))
            .clickable(
                enabled = state.shouldShowPagesDropdown,
            ) {
                coroutineScope.launch {
                    if (modalSheetState.isVisible) {
                        modalSheetState.hide()
                    } else {
                        modalSheetState.show()
                    }
                }
            }
    ) {
        Text(
            text = stringResource(id = string.theme_preview_title),
            style = MaterialTheme.typography.body1,
        )
        if (state.shouldShowPagesDropdown) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.currentPageTitle,
                    style = MaterialTheme.typography.caption,
                )
                Icon(
                    modifier = Modifier
                        .size(dimensionResource(id = dimen.major_100))
                        .padding(
                            start = dimensionResource(id = dimen.minor_50),
                            top = dimensionResource(id = dimen.minor_75),
                        ),
                    painter = painterResource(drawable.ic_arrow_down),
                    contentDescription = "",
                    tint = colorResource(id = color.color_on_surface)
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
private fun ThemeDemoPagesBottomSheet(
    pages: List<ThemeDemoPage>,
    onPageSelected: (ThemeDemoPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
        BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
        Text(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = dimen.major_100),
                    top = dimensionResource(id = dimen.minor_100)
                ),
            text = stringResource(id = string.theme_preview_bottom_sheet_pages_title),
            style = MaterialTheme.typography.h6,
        )
        Text(
            modifier = Modifier
                .padding(
                    start = dimensionResource(id = dimen.major_100),
                    bottom = dimensionResource(id = dimen.minor_100)
                ),
            text = stringResource(id = string.theme_preview_bottom_sheet_pages_subtitle),
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
                        top = dimensionResource(id = dimen.major_85),
                        start = dimensionResource(id = dimen.major_100),
                        end = dimensionResource(id = dimen.major_100),
                        bottom = dimensionResource(id = dimen.major_85)
                    )
            )
            if (index == pages.lastIndex) {
                Spacer(modifier = Modifier.height(dimensionResource(id = dimen.major_150)))
            }
        }
    }
}
