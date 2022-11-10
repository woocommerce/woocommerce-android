package com.woocommerce.android.ui.login.storecreation.mystoresummary

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel

@Composable
fun MyStoreSummaryScreen(viewModel: MyStoreSummaryViewModel) {
    viewModel.viewState.observeAsState(DomainPickerViewModel.DomainPickerState()).value.let { viewState ->
        Scaffold(topBar = {
            Toolbar(
                onArrowBackPressed = viewModel::onBackPressed,
            )
        }) {
            Text(text = "Show store info summary")
        }
    }
}

@Composable
private fun Toolbar(
    onArrowBackPressed: () -> Unit,
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
        elevation = 0.dp,
        modifier = modifier
    )
}
