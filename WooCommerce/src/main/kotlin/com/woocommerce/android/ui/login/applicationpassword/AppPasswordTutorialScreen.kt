package com.woocommerce.android.ui.login.applicationpassword

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

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
    ApplicationPasswordTutorialScreen(
        onContinueClicked = { },
        onContactSupportClicked = { }
    )
}
