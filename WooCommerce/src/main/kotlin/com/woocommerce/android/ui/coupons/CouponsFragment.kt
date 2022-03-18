package com.woocommerce.android.ui.coupons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.composethemeadapter.MdcTheme
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCouponsBinding
import com.woocommerce.android.ui.base.BaseFragment

class CouponsFragment : BaseFragment(R.layout.fragment_coupons) {
    private var _binding: FragmentCouponsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCouponsBinding.inflate(inflater, container, false)

        val view = binding.root
        binding.couponsComposeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MdcTheme {
                    // todo call coupons component
                }
            }
        }
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
