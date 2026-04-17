package com.woocommerce.android.ui.orders.wooshippinglabels.carriertos

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCColoredButton

@Composable
fun CarrierTermsBottomSheetScaffold(
    @StringRes titleResId: Int,
    @StringRes descriptionResId: Int,
    originAddress: String?,
    confirmEnabled: Boolean,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier,
    checkboxes: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(
                topStart = dimensionResource(id = R.dimen.minor_100),
                topEnd = dimensionResource(id = R.dimen.minor_100)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))

                Column(
                    Modifier
                        .fillMaxWidth()
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(id = titleResId),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    originAddress?.let {
                        OriginAddressSection(it)
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text(
                        text = stringResource(id = descriptionResId),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        checkboxes()
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    WCColoredButton(
                        onClick = onContinueClicked,
                        text = stringResource(id = R.string.wpp_shipping_ups_tos_accept),
                        enabled = confirmEnabled,
                        loading = isLoading,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            SnackbarHost(
                snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun OriginAddressSection(
    address: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.wpp_shipping_ups_tos_shipping_from),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = address,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CheckboxWithTitle(
    checked: Boolean,
    title: AnnotatedString,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
