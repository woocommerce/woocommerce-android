package com.woocommerce.android.ui.orders.creation.simplepaymentsmigration

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun OrderCreateEditSimplePaymentsMigrationBottomSheetScreen(
    onAddCustomAmountClicked: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .fillMaxWidth(),
    ) {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_bell_64dp),
            contentDescription =
            stringResource(id = R.string.order_creation_simple_payment_migration_title),
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        Text(
            text = stringResource(id = R.string.order_creation_simple_payment_migration_title),
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        Text(
            text = stringResource(id = R.string.order_creation_simple_payment_migration_message_one),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        Text(
            text = stringResource(id = R.string.order_creation_simple_payment_migration_message_two),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center,
            color = colorResource(R.color.color_surface_variant),
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

        Button(
            onClick = onAddCustomAmountClicked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(id = R.string.order_creation_simple_payment_migration_button))
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewOrderCreateEditSimplePaymentsMigrationBottomSheetScreen() {
    WooThemeWithBackground {
        OrderCreateEditSimplePaymentsMigrationBottomSheetScreen(
            onAddCustomAmountClicked = {},
        )
    }
}
