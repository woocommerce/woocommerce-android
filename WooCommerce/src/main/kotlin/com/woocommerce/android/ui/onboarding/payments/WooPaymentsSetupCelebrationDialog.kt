package com.woocommerce.android.ui.onboarding.payments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class WooPaymentsSetupCelebrationDialog : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooTheme {
                    WooPaymentsSetupCelebrationScreen(
                        onDoneClick = ::navigateBack
                    )
                }
            }
        }
    }

    private fun navigateBack() {
        findNavController().navigateUp()
    }
}

@Composable
private fun WooPaymentsSetupCelebrationScreen(
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.minor_100),
            topEnd = dimensionResource(id = R.dimen.minor_100)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100))
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_50)))
            BottomSheetHandle()
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
            Box(
                modifier = Modifier
                    .size(dimensionResource(id = R.dimen.major_400))
                    .border(
                        width = dimensionResource(id = R.dimen.minor_100),
                        color = colorResource(id = R.color.woo_green_50).copy(alpha = 0.5f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check_rounded),
                    tint = colorResource(id = R.color.woo_green_50),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(width = 42.dp, height = 32.dp)
                )
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))
            Text(
                text = stringResource(id = R.string.store_onboarding_task_woopayments_celebration_header),
                style = MaterialTheme.typography.h6,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            Text(
                text = stringResource(id = R.string.store_onboarding_task_woopayments_celebration_message),
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            WCColoredButton(onClick = onDoneClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.done))
            }
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }
}

@Composable
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
private fun WooPaymentsSetupCelebrationScreenPreview() {
    WooThemeWithBackground {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            WooPaymentsSetupCelebrationScreen(onDoneClick = { })
        }
    }
}
