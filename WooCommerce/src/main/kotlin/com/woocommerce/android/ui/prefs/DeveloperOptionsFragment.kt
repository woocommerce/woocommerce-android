package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentDeveloperOptionsBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeveloperOptionsFragment : BaseFragment(R.layout.fragment_developer_options) {
    private var _binding: FragmentDeveloperOptionsBinding? = null
    private val binding get() = _binding!!
    // private val viewModel: DeveloperOptionsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeveloperOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun getFragmentTitle() = resources.getString(R.string.dev_options)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
