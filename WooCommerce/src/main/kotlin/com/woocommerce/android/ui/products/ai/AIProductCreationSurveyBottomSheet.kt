package com.woocommerce.android.ui.products.ai

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AIProductCreationSurveyBottomSheet : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            WooThemeWithBackground {
                SurveyBottomSheetContent(
                    onStartSurveyClick = {},
                    onSkipPressed = { findNavController().popBackStack() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SurveyBottomSheetContent(
    onStartSurveyClick: () -> Unit,
    onSkipPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(
            start = dimensionResource(id = dimen.major_100),
            end = dimensionResource(id = dimen.major_100),
            top = dimensionResource(id = dimen.major_250),
            bottom = dimensionResource(id = dimen.major_250),
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.major_100)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = drawable.ai_product_creation_survey_icon),
            contentDescription = "", // No relevant content desc
        )
        Text(
            text = stringResource(id = string.blaze_campaign_creation_intro_title),
            style = MaterialTheme.typography.subtitle1
        )
        Text(
            text = stringResource(id = string.blaze_campaign_creation_intro_title),
            style = MaterialTheme.typography.body2,
            color = colorResource(id = color.color_on_surface_medium_selector),
        )
        WCColoredButton(
            onClick = onStartSurveyClick,
            text = stringResource(id = string.product_creation_survey_button_open)
        )
        WCOutlinedButton(
            onClick = onSkipPressed,
            text = stringResource(id = string.product_creation_survey_button_skip)
        )
    }
}

@Composable
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun BlazeCreationIntroPreview() {
    WooThemeWithBackground {
        SurveyBottomSheetContent(
            onStartSurveyClick = {},
            onSkipPressed = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
