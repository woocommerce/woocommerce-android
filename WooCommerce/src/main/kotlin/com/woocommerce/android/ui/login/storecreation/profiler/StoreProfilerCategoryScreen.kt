package com.woocommerce.android.ui.login.storecreation.profiler

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun StoreProfilerCategoryScreen(viewModel: StoreProfilerViewModel) {
    viewModel.storeProfilerState.observeAsState().value?.let { _ ->
        Scaffold(topBar = {
            Toolbar(
                onArrowBackPressed = viewModel::onArrowBackPressed,
                onSkipPressed = viewModel::onSkipPressed
            )
        }) {

        }
    }
}

@Composable
private fun Toolbar(
    onArrowBackPressed: () -> Unit,
    onSkipPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {},
        navigationIcon = {
            IconButton(onClick = onArrowBackPressed) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.back)
                )
            }
        },
        actions = {
            TextButton(onClick = onSkipPressed) {
                Text(text = stringResource(id = R.string.skip))
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
private fun Categories() {

}
