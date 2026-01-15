package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class FirstProductCelebrationDialog : DialogFragment() {
    companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_PORTRAIT_WIDTH_RATIO = 0.7f
    }

    @Inject lateinit var navigator: ProductNavigator

    private val viewModel: FirstProductCelebrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    FirstProductCelebrationScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ProductNavigationTarget -> navigator.navigate(this, event)
                is Exit -> findNavController().navigateUp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isTablet()) {
            val widthRatio = if (DisplayUtils.isLandscape(context)) {
                TABLET_LANDSCAPE_WIDTH_RATIO
            } else {
                TABLET_PORTRAIT_WIDTH_RATIO
            }

            requireDialog().window!!.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * widthRatio).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun isTablet() = DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)
}
