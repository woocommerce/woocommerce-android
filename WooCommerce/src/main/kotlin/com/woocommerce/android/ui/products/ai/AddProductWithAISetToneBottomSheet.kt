package com.woocommerce.android.ui.products.ai

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BottomSheetHandle
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.ai.AddProductWithAIViewModel.AiTone
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class AddProductWithAISetToneBottomSheet : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooTheme {
                    AiToneBottomSheetContent(
                        aiTones = AddProductWithAIViewModel.AiTone.values(),
                        selectedTone = AiTone.Casual,
                        onToneSelected = { /*TODO*/ }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiToneBottomSheetContent(
    aiTones: Array<AiTone>,
    selectedTone: AiTone,
    onToneSelected: (AiTone) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = R.dimen.minor_100),
            topEnd = dimensionResource(id = R.dimen.minor_100)
        )
    ) {
        Column {
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
            BottomSheetHandle(Modifier.align(Alignment.CenterHorizontally))
            Text(
                modifier = Modifier
                    .padding(dimensionResource(id = R.dimen.major_100))
                    .align(Alignment.CenterHorizontally),
                text = stringResource(id = R.string.product_creation_ai_tone_title),
                style = MaterialTheme.typography.h6,
            )
            Divider()
            Text(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
                text = stringResource(id = R.string.product_creation_ai_tone_description),
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
            aiTones.forEachIndexed { index, tone ->
                Row(
                    modifier = Modifier
                        .clickable { onToneSelected(tone) }
                        .fillMaxWidth()
                        .padding(dimensionResource(id = R.dimen.major_100))
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = stringResource(id = tone.displayName),
                        style = MaterialTheme.typography.subtitle1,
                    )
                    if (selectedTone == tone) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = stringResource(
                                id = R.string.product_creation_ai_tone_selected_content_desc
                            ),
                            tint = colorResource(id = R.color.color_primary)
                        )
                    }
                }
                if (index < aiTones.size - 1)
                    Divider(
                        modifier = Modifier.padding(dimensionResource(id = R.dimen.minor_75)),
                    )
            }
        }
    }
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "small screen", device = Devices.PIXEL)
@Preview(name = "mid screen", device = Devices.PIXEL_4)
@Preview(name = "large screen", device = Devices.NEXUS_10)
@Composable
private fun AiToneBottomSheetContentPreview() {
    WooThemeWithBackground {
        AiToneBottomSheetContent(
            aiTones = AddProductWithAIViewModel.AiTone.values(),
            selectedTone = AiTone.Casual,
            onToneSelected = {}
        )
    }
}
