package com.woocommerce.android.ui.products.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import com.woocommerce.android.R.dimen
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment

class AddProductWithAISetToneBottomSheet : WCBottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooTheme {
                    SetAiToneContent()
                }
            }
        }
    }
}

@Composable
private fun SetAiToneContent() {
    Surface(
        shape = RoundedCornerShape(
            topStart = dimensionResource(id = dimen.minor_100),
            topEnd = dimensionResource(id = dimen.minor_100)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text("Tone options")
        }
    }
}
