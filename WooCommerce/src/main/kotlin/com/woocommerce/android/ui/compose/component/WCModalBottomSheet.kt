package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WCModalBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.windowInsets },
    shape: Shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        containerColor = colorResource(R.color.color_surface_elevated),
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        contentWindowInsets = contentWindowInsets,
        shape = shape,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
fun dismissWCModalBottomSheet(
    coroutineScope: CoroutineScope,
    modalSheetState: SheetState,
    invokeOnCompletion: () -> Unit
) {
    coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
        if (!modalSheetState.isVisible) {
            invokeOnCompletion()
        }
    }
}
