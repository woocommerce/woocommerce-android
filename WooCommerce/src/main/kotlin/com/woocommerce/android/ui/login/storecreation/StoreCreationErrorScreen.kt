package com.woocommerce.android.ui.login.storecreation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCColoredButton

@Composable
fun StoreCreationErrorScreen(
    errorType: StoreCreationErrorType,
    onArrowBackPressed: (() -> Unit),
    message: String? = null,
    onRetryButtonClicked: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize()
    ) {
        TopAppBar(
            backgroundColor = MaterialTheme.colors.surface,
            title = {},
            navigationIcon = {
                IconButton(onClick = onArrowBackPressed) {
                    Icon(
                        Filled.ArrowBack,
                        contentDescription = stringResource(id = string.back)
                    )
                }
            },
            elevation = 0.dp
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Image(painter = painterResource(id = drawable.img_woo_generic_error), contentDescription = null)

            Text(
                text = stringResource(id = errorType.title),
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .padding(
                        top = dimensionResource(id = dimen.major_300),
                        start = dimensionResource(id = dimen.major_300),
                        end = dimensionResource(id = dimen.major_300),
                        bottom = dimensionResource(id = dimen.major_100)
                    ),
                textAlign = TextAlign.Center
            )

            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.subtitle1,
                    modifier = Modifier.padding(dimensionResource(id = dimen.major_100)),
                    textAlign = TextAlign.Center
                )
            }

            if (errorType.isRetryPossible && onRetryButtonClicked != null) {
                WCColoredButton(
                    onClick = onRetryButtonClicked,
                    text = stringResource(id = string.retry),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            vertical = dimensionResource(id = dimen.major_150),
                            horizontal = dimensionResource(id = dimen.major_300)
                        )
                )
            }
        }
    }

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
fun StoreCreationErrorPreview() {
    WooThemeWithBackground {
        StoreCreationError(
            errorType = StoreCreationErrorType.SITE_CREATION_FAILED,
            onArrowBackPressed = {},
            message = "Error creating store",
            onRetryButtonClicked = {}
        )
    }
}

