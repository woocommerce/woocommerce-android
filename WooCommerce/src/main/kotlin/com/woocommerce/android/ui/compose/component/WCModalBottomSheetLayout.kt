package com.woocommerce.android.ui.compose.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import com.woocommerce.android.R

/**
 * A wrapper around [ModalBottomSheetLayout] that provides default values for the sheet shape and scrim color.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun WCModalBottomSheetLayout(
    sheetState: ModalBottomSheetState,
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetShape: Shape = RoundedCornerShape(
        topStart = dimensionResource(id = R.dimen.corner_radius_large),
        topEnd = dimensionResource(id = R.dimen.corner_radius_large)
    ),
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetContent = sheetContent,
        sheetShape = sheetShape,
        sheetState = sheetState,
        modifier = modifier,
        scrimColor =
        // Overriding scrim color for dark theme because of the following bug affecting ModalBottomSheetLayout:
        // https://issuetracker.google.com/issues/183697056
        if (isSystemInDarkTheme()) {
            colorResource(id = R.color.color_scrim_background)
        } else {
            ModalBottomSheetDefaults.scrimColor
        },
        content = content,
    )
}
