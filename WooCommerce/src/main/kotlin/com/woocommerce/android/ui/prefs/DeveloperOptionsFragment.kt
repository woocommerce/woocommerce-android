package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentDeveloperOptionsBinding
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeveloperOptionsFragment : BaseFragment(R.layout.fragment_developer_options) {
    val viewModel: DeveloperOptionsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentDeveloperOptionsBinding.bind(view)

        initViews(binding)
        observeViewState(binding)
    }

    private fun observeViewState(binding: FragmentDeveloperOptionsBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { state ->
            (binding.developerOptionsRv.adapter as DeveloperOptionsAdapter).setItems(state.rows)
        }
    }

    private fun initViews(binding: FragmentDeveloperOptionsBinding) {
        binding.developerOptionsRv.layoutManager = LinearLayoutManager(requireContext())
        binding.developerOptionsRv.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        binding.developerOptionsRv.adapter = DeveloperOptionsAdapter()
    }

    override fun getFragmentTitle() = resources.getString(R.string.dev_options)
}
