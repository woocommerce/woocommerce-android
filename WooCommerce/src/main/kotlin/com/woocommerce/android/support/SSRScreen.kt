package com.woocommerce.android.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R

@Composable
fun SSRScreen(viewModel: SSRActivityViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        StatusReportScreen(
            title = stringResource(id = R.string.support_system_status_report),
            isLoading = it.isLoading,
            reportText = it.formattedSSR,
            copyContentDescription = stringResource(id = R.string.support_system_status_report_copy_label),
            shareContentDescription = stringResource(id = R.string.support_system_status_report_share_label),
            onBackPressed = viewModel::onBackPressed,
            onCopyButtonClick = viewModel::onCopyButtonClicked,
            onShareButtonClick = viewModel::onShareButtonClicked
        )
    }
}
