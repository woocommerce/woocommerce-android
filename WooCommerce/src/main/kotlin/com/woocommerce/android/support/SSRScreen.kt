package com.woocommerce.android.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R

@Composable
fun SSRScreen(viewModel: SSRActivityViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        SSRScreen(
            isLoading = it.isLoading,
            formattedSSR = it.formattedSSR,
            onBackPressed = viewModel::onBackPressed,
            onCopyButtonClick = viewModel::onCopyButtonClicked,
            onShareButtonClick = viewModel::onShareButtonClicked
        )
    }
}

@Composable
fun SSRScreen(
    isLoading: Boolean,
    formattedSSR: String,
    onBackPressed: () -> Unit,
    onCopyButtonClick: () -> Unit,
    onShareButtonClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = MaterialTheme.colors.surface,
                title = { Text(stringResource(id = R.string.support_system_status_report)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCopyButtonClick, enabled = !isLoading) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_copy_white_24dp),
                            contentDescription = stringResource(id = R.string.support_system_status_report_copy_label),
                            tint = colorResource(id = R.color.color_icon_menu),
                        )
                    }
                    IconButton(onClick = onShareButtonClick, enabled = !isLoading) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = stringResource(id = R.string.support_system_status_report_share_label),
                            tint = colorResource(id = R.color.color_icon_menu)
                        )
                    }
                }
            )
        }
    ) {
        val scrollState = rememberScrollState()

        // Column is used here despite just having one child component, so that SSRContent can use `weight` Modifier.
        // This allows `CircularProgressIndicator` in the loading state to be centered vertically and horizontally.
        Column {
            SSRContent(
                isLoading = isLoading,
                formattedSSR = formattedSSR,
                modifier = Modifier
                    .background(color = MaterialTheme.colors.surface)
                    .verticalScroll(scrollState)
                    .padding(vertical = dimensionResource(id = R.dimen.major_100))
                    .fillMaxSize()
                    .weight(1.0f)
            )
        }
    }
}

@Composable
fun SSRContent(isLoading: Boolean, formattedSSR: String, modifier: Modifier) {
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
                    text = formattedSSR,
                    modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
                )
            }
        }
    }
}

@Preview
@Composable
fun SSRScreenPreview() {
    SSRScreen(
        isLoading = false,
        formattedSSR = "This is the example SSR content.",
        onBackPressed = {},
        onCopyButtonClick = {},
        onShareButtonClick = {},
    )
}
