package com.woocommerce.android.ui.coupons.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField

@Composable
fun EmailRestrictionScreen(viewModel: EmailRestrictionViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        EmailRestrictionScreen(
            viewState = it,
            onAllowedEmailsChanged = viewModel::onAllowedEmailsChanged
        )
    }
}

@Composable
fun EmailRestrictionScreen(
    viewState: EmailRestrictionViewModel.ViewState,
    onAllowedEmailsChanged: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
        modifier = Modifier
            .background(color = MaterialTheme.colors.surface)
            .verticalScroll(scrollState)
            .padding(vertical = dimensionResource(id = R.dimen.major_100))
            .fillMaxSize()
    ) {
        WCOutlinedTextField(
            value = viewState.allowedEmails,
            label = stringResource(id = R.string.coupon_restrictions_allowed_emails),
            onValueChange = onAllowedEmailsChanged,
            helperText = stringResource(id = R.string.coupon_restrictions_allowed_emails_hint),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        )
    }
}
