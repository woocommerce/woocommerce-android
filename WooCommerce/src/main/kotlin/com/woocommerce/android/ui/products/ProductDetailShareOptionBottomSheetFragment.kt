package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentProductDetailShareOptionBinding
import javax.inject.Inject

class ProductDetailShareOptionBottomSheetFragment : BottomSheetDialogFragment() {
    @Inject lateinit var navigator: ProductNavigator

    val viewModel: ProductDetailViewModel by hiltNavGraphViewModels(R.id.nav_graph_products)

    private var _binding: FragmentProductDetailShareOptionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductDetailShareOptionBinding.inflate(inflater, container, false)

        val view = binding.root
        binding.shareOptionComposeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    ProductDetailShareOption(viewModel)
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ProductNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
