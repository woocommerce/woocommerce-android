package com.woocommerce.android.ui.woopos.settings.details

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsState
import com.woocommerce.android.ui.woopos.settings.details.hardware.WooPosHardwareSettingsScreen

@Composable
fun WooPosSettingsDetailPaneScreen(
    state: WooPosSettingsState,
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDestination = state.currentDestination

    BackHandler(enabled = state.canGoBack) {
        onBack()
    }

    Column(
        modifier = modifier.fillMaxSize()
            .padding(WooPosSpacing.Medium.value)
            .statusBarsPadding()
    ) {
        DetailPaneToolbar(
            canGoBack = state.canGoBack,
            title = state.currentDestination.titleRes,
            onBack = onBack
        )

        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                val isNavigatingForward = targetState.parentDestination == initialState
                val isCategorySwitch = initialState.parentDestination == null && targetState.parentDestination == null

                if (isCategorySwitch) {
                    fadeIn() togetherWith fadeOut()
                } else if (isNavigatingForward) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            },
            label = "settings_detail_animation"
        ) { destination ->
            when (destination) {
                is WooPosSettingsDetailDestination.Hardware.Overview -> {
                    WooPosHardwareSettingsScreen(onNavigate = onNavigate)
                }
                is WooPosSettingsDetailDestination.Hardware.BarcodeScanners -> {
                    BarcodeScannerDetailScreen()
                }
                is WooPosSettingsDetailDestination.Hardware.CardReaders -> {
                    CardReadersDetailScreen()
                }
                is WooPosSettingsDetailDestination.Store.Overview -> {
                    StoreDetailScreen()
                }
            }
        }
    }
}

@Composable
private fun DetailPaneToolbar(
    canGoBack: Boolean,
    @StringRes title: Int,
    onBack: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(
                top = WooPosSpacing.Small.value,
                bottom = WooPosSpacing.Medium.value
            )
    ) {
        val (backButton, titleText) = createRefs()

        AnimatedVisibility(
            visible = canGoBack,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.constrainAs(backButton) {
                start.linkTo(parent.start)
                centerVerticallyTo(parent)
            }
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.woopos_toolbar_icon_content_description),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Box(
            modifier = Modifier.constrainAs(titleText) {
                start.linkTo(
                    if (canGoBack) backButton.end else parent.start,
                    margin = if (canGoBack) WooPosSpacing.XSmall.value else 0.dp
                )
                end.linkTo(parent.end)
                centerVerticallyTo(parent)
                width = Dimension.fillToConstraints
            }
        ) {
            WooPosText(
                text = stringResource(title),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BarcodeScannerDetailScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Medium.value),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_barcode_scanner_detail_title),
            style = WooPosTypography.Heading
        )
    }
}

@Composable
private fun CardReadersDetailScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Medium.value),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_card_reader_detail_title),
            style = WooPosTypography.Heading
        )
    }
}

@Composable
private fun StoreDetailScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Medium.value),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_store_category),
            style = WooPosTypography.Heading
        )
    }
}
