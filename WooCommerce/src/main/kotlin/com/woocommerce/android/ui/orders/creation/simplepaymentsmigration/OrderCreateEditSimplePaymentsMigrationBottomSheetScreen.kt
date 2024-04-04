package com.woocommerce.android.ui.orders.creation.simplepaymentsmigration

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
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
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooTheme

@Composable
fun OrderCreateEditSimplePaymentsMigrationBottomSheetScreen(
    onAddCustomAmountClicked: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.minor_100),
            topEnd = dimensionResource(id = R.dimen.minor_100)
        )
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(id = R.dimen.minor_100))
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))

            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_bell_64dp),
                contentDescription = stringResource(id = R.string.order_creation_simple_payment_migration_title),
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))

            Text(
                text = stringResource(id = R.string.order_creation_simple_payment_migration_title),
                style = MaterialTheme.typography.h5,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

            Text(
                text = stringResource(id = R.string.order_creation_simple_payment_migration_message_one),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))

            Text(
                text = stringResource(id = R.string.order_creation_simple_payment_migration_message_two),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                color = colorResource(R.color.color_surface_variant),
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_150)))

            WCColoredButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAddCustomAmountClicked,
                text = stringResource(id = R.string.order_creation_simple_payment_migration_button),
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
fun PreviewOrderCreateEditSimplePaymentsMigrationBottomSheetScreen() {
    WooTheme {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            OrderCreateEditSimplePaymentsMigrationBottomSheetScreen(
                onAddCustomAmountClicked = {},
            )
        }
    }
}
