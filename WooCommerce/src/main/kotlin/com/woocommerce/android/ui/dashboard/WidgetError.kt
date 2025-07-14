package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.WCOutlinedButton

@Composable
fun WidgetError(
    onContactSupportClicked: () -> Unit,
    onRetryClicked: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            modifier = Modifier
                .align(Alignment.CenterHorizontally),
            contentDescription = null,
            painter = painterResource(id = R.drawable.img_widget_error)
        )

        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(id = R.string.dynamic_dashboard_widget_error_title),
            style = MaterialTheme.typography.h6
        )

        val errorMessage = annotatedStringRes(
            stringResId = R.string.dynamic_dashboard_widget_error_description,
            onUrlClick = { onContactSupportClicked() }
        )
        Text(
            text = errorMessage,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = LocalContentColor.current,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        WCOutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRetryClicked
        ) {
            Text(text = stringResource(id = R.string.retry))
        }
    }
}

@Composable
@Preview
fun WidgetErrorPreview() {
    WidgetError(
        onContactSupportClicked = {},
        onRetryClicked = {}
    )
}
