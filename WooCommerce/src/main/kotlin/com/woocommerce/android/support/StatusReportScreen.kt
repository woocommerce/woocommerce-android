package com.woocommerce.android.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar

/**
 * Renders a plain-text status report with copy and share actions. Shared by the server-side System Status Report
 * and the app-side Mobile Status Report, which differ only in their title and in where the text comes from.
 */
@Composable
fun StatusReportScreen(
    title: String,
    isLoading: Boolean,
    reportText: String,
    copyContentDescription: String,
    shareContentDescription: String,
    onBackPressed: () -> Unit,
    onCopyButtonClick: () -> Unit,
    onShareButtonClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                title = title,
                onNavigationButtonClick = onBackPressed,
                actions = {
                    IconButton(onClick = onCopyButtonClick, enabled = !isLoading) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy_white_24dp),
                            contentDescription = copyContentDescription,
                            tint = colorResource(id = R.color.color_icon_menu),
                        )
                    }
                    IconButton(onClick = onShareButtonClick, enabled = !isLoading) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_share_24dp),
                            contentDescription = shareContentDescription,
                            tint = colorResource(id = R.color.color_icon_menu)
                        )
                    }
                },
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.color_toolbar))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
    ) { padding ->
        val scrollState = rememberScrollState()

        // Column is used here despite just having one child component, so that StatusReportContent can use `weight`
        // Modifier. This allows `CircularProgressIndicator` in the loading state to be centered vertically and
        // horizontally.
        Column {
            StatusReportContent(
                isLoading = isLoading,
                reportText = reportText,
                modifier = Modifier
                    .background(color = MaterialTheme.colors.surface)
                    .verticalScroll(scrollState)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                    .padding(padding)
                    .padding(vertical = dimensionResource(id = R.dimen.major_100))
                    .fillMaxSize()
                    .weight(1.0f)
            )
        }
    }
}

@Composable
private fun StatusReportContent(isLoading: Boolean, reportText: String, modifier: Modifier) {
    Box(
        modifier = modifier
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
            )
        } else {
            SelectionContainer {
                Text(
                    text = reportText,
                    modifier = Modifier
                        .padding(dimensionResource(R.dimen.major_100))
                )
            }
        }
    }
}

@Preview
@Composable
private fun StatusReportScreenPreview() {
    StatusReportScreen(
        title = stringResource(id = R.string.support_mobile_status_report),
        isLoading = false,
        reportText = "This is the example report content.",
        copyContentDescription = "",
        shareContentDescription = "",
        onBackPressed = {},
        onCopyButtonClick = {},
        onShareButtonClick = {},
    )
}
