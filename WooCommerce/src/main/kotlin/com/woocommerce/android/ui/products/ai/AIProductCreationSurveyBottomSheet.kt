package com.woocommerce.android.ui.products.ai

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.lang.reflect.Modifier

@AndroidEntryPoint
class AIProductCreationSurveyBottomSheet : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            WooThemeWithBackground {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dimensionResource(id = R.dimen.major_100)),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_100)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ai_product_creation_survey_icon),
                        contentDescription = "", // No relevant content desc
                    )
                    Text(
                        text = stringResource(id = R.string.blaze_campaign_creation_intro_title),
                        style = MaterialTheme.typography.subtitle1
                    )
                    Text(
                        text = stringResource(id = R.string.blaze_campaign_creation_intro_title),
                        style = MaterialTheme.typography.body2,
                        color = colorResource(id = R.color.color_on_surface_medium_selector),
                    )
                    WCColoredButton(
                        onClick = { /*TODO*/ },
                        text = stringResource(id = R.string.product_creation_survey_button_open)
                    )
                    WCOutlinedButton(
                        onClick = { findNavController().popBackStack() },
                        text = stringResource(id = R.string.product_creation_survey_button_skip)
                    )
                }
            }
        }
    }

//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        observeEvents()
//    }
}
