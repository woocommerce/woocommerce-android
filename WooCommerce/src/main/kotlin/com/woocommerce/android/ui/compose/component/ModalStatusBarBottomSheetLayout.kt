package com.woocommerce.android.ui.compose.component

import android.R.attr
import android.util.TypedValue
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue.Hidden
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat
import com.woocommerce.android.R
import com.woocommerce.android.extensions.findActivity

/*
 * This is a custom implementation of the ModalBottomSheetLayout that fixes the scrim color of the status bar
 * and the show animation.
 *
 * Source: https://stackoverflow.com/a/76998328
 *
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)
@Composable
fun ModalStatusBarBottomSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState =
        rememberModalBottomSheetState(Hidden),
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    sheetBackgroundColor: Color = colorResource(id = R.color.bottom_sheet_background),
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    content: @Composable () -> Unit
): Unit = ModalBottomSheetLayout(
    sheetContent = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            sheetContent.invoke(this@ModalBottomSheetLayout)
        }
    },
    modifier = modifier
        .imePadding()
        .navigationBarsPadding()
        .imeNestedScroll(),
    sheetState = sheetState,
    sheetShape = sheetShape,
    sheetElevation = sheetElevation,
    sheetBackgroundColor = sheetBackgroundColor,
    sheetContentColor = sheetContentColor,
    scrimColor = scrimColor(),
) {
    val context = LocalContext.current
    var statusBarColor by remember { mutableStateOf(Color.Transparent) }
    val backgroundColor = remember {
        val typedValue = TypedValue()
        if (context.findActivity()?.theme?.resolveAttribute(attr.windowBackground, typedValue, true) == true) {
            Color(typedValue.data)
        } else {
            sheetBackgroundColor
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(statusBarColor)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor)
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            content()
        }
    }

    val window = remember { context.findActivity()?.window }
    if (window == null) return@ModalBottomSheetLayout

    val originalNavigationBarColor = remember { window.navigationBarColor }

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue != Hidden) {
            window.navigationBarColor = sheetBackgroundColor.toArgb()
        } else {
            window.navigationBarColor = originalNavigationBarColor
        }
    }

    DisposableEffect(Unit) {
        val originalStatusBarColor = window.statusBarColor
        statusBarColor = Color(originalStatusBarColor)

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        onDispose {
            window.statusBarColor = originalStatusBarColor
            window.navigationBarColor = originalNavigationBarColor
            WindowCompat.setDecorFitsSystemWindows(window, true)
        }
    }
}

@Composable
private fun scrimColor() = if (isSystemInDarkTheme()) {
    colorResource(id = R.color.color_scrim_background)
} else {
    ModalBottomSheetDefaults.scrimColor
}
