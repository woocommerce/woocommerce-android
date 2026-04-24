package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewFontScale
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosFullScreenInputLayout(
    modifier: Modifier = Modifier,
    titleText: String,
    onBackClicked: () -> Unit,
    centerContent: @Composable () -> Unit,
    buttonText: String,
    buttonState: WooPosButtonState = WooPosButtonState.ENABLED,
    onButtonClicked: () -> Unit,
    topContent: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null
) {
    BackHandler { onBackClicked() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        WooPosToolbar(
            titleText = titleText,
            onBackClicked = onBackClicked,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            topContent?.invoke()

            Spacer(modifier = Modifier.weight(1f))

            centerContent()

            if (bottomContent != null) {
                bottomContent()
            }

            Spacer(modifier = Modifier.weight(1f))

            WooPosButton(
                text = buttonText,
                onClick = onButtonClicked,
                state = buttonState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Medium.value)
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))
        }
    }
}

@Composable
@PreviewFontScale
fun WooPosFullScreenInputLayoutPreview() {
    WooPosTheme {
        WooPosFullScreenInputLayout(
            titleText = "Enter Information",
            onBackClicked = {},
            centerContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WooPosText(
                        text = "Sample Input",
                        style = WooPosTypography.Heading,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            buttonText = "Save",
            onButtonClicked = {}
        )
    }
}
