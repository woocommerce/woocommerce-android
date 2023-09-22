package com.woocommerce.android.ui.products.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.ai.AboutProductSubViewModel.NavigateToAiToneBottomSheet
import com.woocommerce.android.ui.products.ai.AddProductWithAISetToneBottomSheet.Companion.KEY_PRODUCT_CREATION_AI_TONE
import com.woocommerce.android.ui.products.ai.AddProductWithAISetToneViewModel.AiTone
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddProductWithAIFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: AddProductWithAIViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    AddProductWithAIScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        handleEvents()
        handleResults()
    }

    private fun handleResults() {
        handleResult<AiTone>(KEY_PRODUCT_CREATION_AI_TONE) { aiTone ->
            viewModel.updateAiTone(aiTone)
        }
    }

    private fun handleEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                Exit -> findNavController().navigateUp()
                is NavigateToAiToneBottomSheet -> openAiToneBottomSheet(event.aiTone)
            }
        }
    }

    private fun openAiToneBottomSheet(aiTone: AiTone) {
        findNavController().navigate(
            AddProductWithAIFragmentDirections
                .actionAddProductWithAIFragmentToAddProductWithAISetToneBottomSheet(aiTone)
        )
    }
}
