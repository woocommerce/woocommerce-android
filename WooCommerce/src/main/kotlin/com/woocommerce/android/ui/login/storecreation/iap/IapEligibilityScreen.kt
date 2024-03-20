package com.woocommerce.android.ui.login.storecreation.iap

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R

@Composable
fun IapEligibilityScreen(viewModel: IapEligibilityViewModel) {
    viewModel.isCheckingIapEligibility.observeAsState().value?.let { isLoading ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.major_100)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            }
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_200)),
                text =
                stringResource(
                    id = if (isLoading) {
                        R.string.store_creation_iap_eligibility_loading_title
                    } else {
                        R.string.store_creation_iap_eligibility_check_error_title
                    }
                ),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center,
            )
            if (isLoading) {
                Text(
                    modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)),
                    text = stringResource(id = R.string.store_creation_iap_eligibility_loading_subtitle),
                    style = MaterialTheme.typography.subtitle1,
                    color = colorResource(id = R.color.color_on_surface_medium)
                )
            }
        }
    }
}
