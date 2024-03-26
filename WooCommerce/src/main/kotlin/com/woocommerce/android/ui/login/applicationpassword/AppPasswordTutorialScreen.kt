package com.woocommerce.android.ui.login.applicationpassword

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ApplicationPasswordTutorialScreen(viewModel: AppPasswordTutorialViewModel) {
    ApplicationPasswordTutorialScreen(
        onContinueClicked = viewModel::onContinueClicked,
        onContactSupportClicked = viewModel::onContactSupportClicked
    )
}

@Composable
fun ApplicationPasswordTutorialScreen(
    modifier: Modifier = Modifier,
    onContinueClicked: () -> Unit,
    onContactSupportClicked: () -> Unit
) {
    Column(modifier) {
        Text("We couldn't log in into your store")
        Text("This could because your store has some extra security steps in place.")
        Divider()
        Text("Follow these steps to connect the Woo app directly to your store using an application password.")
        Text("1. First, log in using your site credentials.")
        Text("2. When prompted, approve the connection by tapping the confirmation button.")
        Image(
            painter = painterResource(id = R.drawable.stats_today_widget_preview),
            contentDescription = null
        )
        Divider()

        Button(onClick = onContinueClicked) {
            Text("Continue")
        }

        Button(onClick = onContactSupportClicked) {
            Text("Contact support")
        }
    }
}

@Preview
@Composable
fun ApplicationPasswordTutorialScreenPreview() {
    WooThemeWithBackground {
        ApplicationPasswordTutorialScreen(
            onContinueClicked = { },
            onContactSupportClicked = { }
        )
    }
}
