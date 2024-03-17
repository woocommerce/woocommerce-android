package com.woocommerce.android.ui.orders.list

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CreateTestOrderDialog(
    onStartTestOrderButtonClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = colorResource(id = R.color.color_surface)
            )
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                modifier = Modifier.padding(top = dimensionResource(id = R.dimen.major_100)),
                text = stringResource(id = R.string.try_test_order_heading),
                style = MaterialTheme.typography.h5,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_300)))

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val imageHeight = if (isLandscape) 100.dp else 150.dp

            Image(
                painter = painterResource(R.drawable.img_create_test_order),
                contentDescription = "",
                modifier = Modifier.height(imageHeight)
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))

            TestOrderStep(1, R.string.try_test_order_step_1)
            TestOrderStep(2, R.string.try_test_order_step_2)
            TestOrderStep(3, R.string.try_test_order_step_3)
            TestOrderStep(4, R.string.try_test_order_step_4)

            WCColoredButton(
                modifier = Modifier
                    .padding(top = dimensionResource(id = R.dimen.major_100))
                    .fillMaxWidth(),
                onClick = onStartTestOrderButtonClick
            ) {
                Text(stringResource(id = R.string.try_test_order_button))
            }
        }
    }
}

@Composable
fun TestOrderStep(stepNumber: Int, stepTextId: Int) {
    val format = NumberFormat.getInstance(Locale.getDefault())
    val formattedNumber = format.format(stepNumber)
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.major_200))
                    .background(
                        color = colorResource(R.color.try_test_order_step),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = formattedNumber)
            }

            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))

            Text(text = stringResource(id = stepTextId))
        }

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Landscape mode", widthDp = 640, heightDp = 360, showBackground = true)
@Composable
fun PreviewCreateTestOrderDialog() {
    WooThemeWithBackground {
        CreateTestOrderDialog {}
    }
}
