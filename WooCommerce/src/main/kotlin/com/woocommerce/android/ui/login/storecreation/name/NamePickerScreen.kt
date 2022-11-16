package com.woocommerce.android.ui.login.storecreation.name

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun NamePickerScreen(viewModel: NamePickerViewModel) {
    viewModel.storeName.observeAsState().value.let { storeName ->
        Scaffold(topBar = {
            Toolbar(
                onCancelPressed = viewModel::onCancelPressed
            )
        }) {

        }
    }
}

@Composable
private fun Toolbar(
    onCancelPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        title = {},
        navigationIcon = {
            TextButton(onClick = onCancelPressed) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        elevation = 0.dp,
        modifier = modifier
    )
}
