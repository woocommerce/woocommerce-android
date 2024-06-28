package com.woocommerce.android.ui.products.ai

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.compose.URL_ANNOTATION_TAG
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class AddProductWithAIBottomSheet : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooTheme {
                    AddProductWithAIContent(
                        onAIClick = { showAICreationFlow() },
                        onManualClick = { showManualCreationFlow() },
                        onLearnMoreClick = { openAIGuidelinesPage() }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnalyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_ENTRY_POINT_DISPLAYED)
    }

    private fun showAICreationFlow() {
        AnalyticsTracker.track(AnalyticsEvent.PRODUCT_CREATION_AI_ENTRY_POINT_TAPPED)
        val action = when (FeatureFlag.PRODUCT_CREATION_WITH_AI_V2.isEnabled()) {
            true -> AddProductWithAIBottomSheetDirections
                .actionAddProductWithAIBottomSheetToAddProductWithAIFragmentV2()

            else -> AddProductWithAIBottomSheetDirections
                .actionAddProductWithAIBottomSheetToAddProductWithAIFragment()
        }
        findNavController().navigateSafely(action)
    }

    private fun showManualCreationFlow() {
        findNavController().navigateSafely(
            AddProductWithAIBottomSheetDirections
                .actionAddProductWithAIBottomSheetToProductTypesBottomSheetFragment(
                    isAddProduct = true
                )
        )
    }

    private fun openAIGuidelinesPage() {
        ChromeCustomTabUtils.launchUrl(
            requireContext(),
            AppUrls.AUTOMATTIC_AI_GUIDELINES
        )
    }
}

@Composable
private fun AddProductWithAIContent(
    onAIClick: () -> Unit,
    onManualClick: () -> Unit,
    onLearnMoreClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.minor_100),
            topEnd = dimensionResource(id = R.dimen.minor_100)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
            Text(
                text = stringResource(id = R.string.product_creation_ai_entry_sheet_header),
                color = colorResource(id = R.color.color_on_surface_medium),
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
            )

            val subtitleId = when (FeatureFlag.PRODUCT_CREATION_WITH_AI_V2.isEnabled()) {
                true -> R.string.product_creation_ai_entry_sheet_ai_option_subtitle_v2
                else -> R.string.product_creation_ai_entry_sheet_ai_option_subtitle
            }
            BottomSheetButton(
                title = stringResource(id = R.string.product_creation_ai_entry_sheet_ai_option_title),
                subtitle = stringResource(id = subtitleId),
                icon = painterResource(id = R.drawable.ic_ai),
                onClick = onAIClick,
                iconTint = MaterialTheme.colors.primary
            )

            Spacer(Modifier.height(dimensionResource(id = R.dimen.minor_100)))

            val learnMoreText = annotatedStringRes(stringResId = R.string.product_creation_ai_entry_sheet_learn_more)
            ClickableText(
                text = learnMoreText,
                style = MaterialTheme.typography.caption.copy(
                    color = colorResource(id = R.color.color_on_surface_medium)
                ),
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.major_325))
            ) {
                learnMoreText.getStringAnnotations(tag = URL_ANNOTATION_TAG, start = it, end = it)
                    .firstOrNull()
                    ?.let { onLearnMoreClick() }
            }

            Divider(Modifier.padding(dimensionResource(id = R.dimen.major_100)))

            BottomSheetButton(
                title = stringResource(id = R.string.product_creation_ai_entry_sheet_manual_option_title),
                subtitle = stringResource(id = R.string.product_creation_ai_entry_sheet_manual_option_subtitle),
                icon = painterResource(id = R.drawable.ic_gridicon_circle_plus),
                onClick = onManualClick
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
        }
    }
}

@Composable
private fun BottomSheetButton(
    title: String,
    subtitle: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Unspecified
) {
    Row(
        modifier
            .clickable(onClick = onClick)
            .padding(
                vertical = dimensionResource(id = R.dimen.minor_100),
                horizontal = dimensionResource(id = R.dimen.major_100)
            )
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(dimensionResource(id = R.dimen.image_minor_50))
        )

        Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.major_100)))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_high)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.body2,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }
    }
}

@Composable
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
private fun AddProductWithAIContentPreview() {
    WooThemeWithBackground {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxSize()
        ) {
            AddProductWithAIContent(
                onAIClick = {},
                onManualClick = {},
                onLearnMoreClick = {},
            )
        }
    }
}
