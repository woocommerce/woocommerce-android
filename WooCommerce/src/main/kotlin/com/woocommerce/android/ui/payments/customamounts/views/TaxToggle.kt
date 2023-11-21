package com.woocommerce.android.ui.payments.customamounts.views

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel

@Composable
fun TaxToggle(
    taxStatus: CustomAmountsDialogViewModel.TaxStatus,
    onCheckedChange: (Boolean) -> Unit
) {
    Row {
        Text(text = stringResource(id = taxStatus.text))
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = taxStatus.isTaxable,
            onCheckedChange = onCheckedChange
        )
    }
}
