package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentComponetsListBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.widgets.AlignedDividerDecoration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CompositeProductFragment : BaseFragment(R.layout.fragment_componets_list) {
    private var _binding: FragmentComponetsListBinding? = null
    private val binding get() = _binding!!

    override fun getFragmentTitle() = resources.getString(R.string.product_components)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentComponetsListBinding.bind(view)

        binding.productsRecycler.run {
            layoutManager = LinearLayoutManager(requireActivity())
            //adapter = productListAdapter
            isMotionEventSplittingEnabled = false
            if (itemDecorationCount == 0) {
                addItemDecoration(
                    AlignedDividerDecoration(
                        context,
                        DividerItemDecoration.VERTICAL,
                        R.id.productInfoContainer
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
