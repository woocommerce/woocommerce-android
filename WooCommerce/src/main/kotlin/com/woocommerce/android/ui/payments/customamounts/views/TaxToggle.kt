package com.woocommerce.android.ui.payments.customamounts.views

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsDialogViewModel

@Composable
fun TaxToggle(
    taxStatus: CustomAmountsDialogViewModel.TaxStatus,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.color_surface))
            .padding(start = 8.dp, end = 8.dp)
    ) {
        Text(
            text = stringResource(id = taxStatus.text),
            style = MaterialTheme.typography.subtitle1
        )
        Spacer(modifier = Modifier.width(8.dp).weight(1f))
        Switch(
            checked = taxStatus.isTaxable,
            onCheckedChange = onCheckedChange
        )
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TaxTogglePreview() {
    WooThemeWithBackground {
        TaxToggle(
            CustomAmountsDialogViewModel.TaxStatus(isTaxable = true)
        ) {}
    }
}
