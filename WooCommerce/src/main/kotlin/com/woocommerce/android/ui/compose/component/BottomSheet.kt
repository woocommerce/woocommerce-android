package com.woocommerce.android.ui.compose.component

import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.dimensionResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.launch

// Credit: https://proandroiddev.com/jetpack-compose-bottom-sheet-over-android-view-using-kotlin-extension-7fecfa8fe369
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetWrapper(
    parent: ViewGroup,
    composeView: ComposeView,
    onDismissed: () -> Unit,
    content: @Composable (dismiss: () -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val modalBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden, skipHalfExpanded = true)
    var isSheetOpened by remember { mutableStateOf(false) }

    ModalBottomSheetLayout(
        sheetBackgroundColor = MaterialTheme.colors.surface,
        sheetState = modalBottomSheetState,
        sheetElevation = dimensionResource(id = R.dimen.minor_100),
        sheetContent = {
            WooThemeWithBackground {
                content(
                    dismiss = {
                        // Action passed for clicking close button in the content
                        coroutineScope.launch {
                            modalBottomSheetState.hide() // will trigger the LaunchedEffect
                            onDismissed()
                        }
                    }
                )
            }
        }
    ) {}

    BackHandler {
        coroutineScope.launch {
            modalBottomSheetState.hide() // will trigger the LaunchedEffect
            onDismissed()
        }
    }

    // Take action based on hidden state
    LaunchedEffect(modalBottomSheetState.currentValue) {
        when (modalBottomSheetState.currentValue) {
            ModalBottomSheetValue.Hidden -> {
                when {
                    isSheetOpened -> parent.removeView(composeView)
                    else -> {
                        isSheetOpened = true
                        coroutineScope.launch {
                            modalBottomSheetState.show()
                        }
                    }
                }
            }
            else -> {
                WooLog.i(T.UTILS, "Bottom sheet ${modalBottomSheetState.currentValue} state")
            }
        }
    }
}
