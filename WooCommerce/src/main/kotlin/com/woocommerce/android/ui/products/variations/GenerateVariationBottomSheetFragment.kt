package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.woocommerce.android.databinding.FragmentGenerateVariationsBottomSheetBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.products.variations.GenerateVariationBottomSheetViewModel.AddNewVariation
import com.woocommerce.android.ui.products.variations.GenerateVariationBottomSheetViewModel.GenerateAllVariations
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GenerateVariationBottomSheetFragment : WCBottomSheetDialogFragment() {
    companion object {
        const val KEY_ADD_NEW_VARIATION = "add_new_variation"
        const val KEY_GENERATE_ALL_VARIATIONS = "generate_all_variations"
    }
    private val viewModel: GenerateVariationBottomSheetViewModel by viewModels()

    private var _binding: FragmentGenerateVariationsBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenerateVariationsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is AddNewVariation -> {
                    navigateBackWithNotice(KEY_ADD_NEW_VARIATION)
                }
                is GenerateAllVariations -> {
                    navigateBackWithNotice(KEY_GENERATE_ALL_VARIATIONS)
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun initializeViews() {
        binding.allVariation.setOnClickListener {
            viewModel.onGenerateAllVariationsClicked()
        }

        binding.newVariation.setOnClickListener {
            viewModel.onAddNewVariationClicked()
        }
    }
}
