package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.clickableAnnotatedStringRes
import com.woocommerce.android.ui.compose.component.WCTextButton

@Composable
fun DateTypeInfoBottomSheet(
    isLoading: Boolean,
    dateTypeLabel: String?,
    onDoneTapped: () -> Unit,
    onOpenSettingsTapped: () -> Unit,
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(id = R.dimen.major_100))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.dashboard_date_type_info_title),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            WCTextButton(onClick = onDoneTapped) {
                Text(
                    text = stringResource(id = R.string.dashboard_date_type_info_done),
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Divider()
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimensionResource(id = R.dimen.major_200)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(dimensionResource(id = R.dimen.major_200)),
                    color = colorResource(id = R.color.color_primary)
                )
            }
        } else {
            Text(
                text = stringResource(
                    id = R.string.dashboard_date_type_info_body,
                    dateTypeLabel ?: ""
                ),
                style = MaterialTheme.typography.body1,
                color = colorResource(id = R.color.color_on_surface_medium),
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
            )
            Text(
                text = clickableAnnotatedStringRes(
                    stringResId = R.string.dashboard_date_type_info_link,
                    onUrlClick = { onOpenSettingsTapped() }
                ),
                style = MaterialTheme.typography.body1,
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100))
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    }
}
