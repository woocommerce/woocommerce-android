package com.woocommerce.android.ui.cardreader.hub

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentCardReaderHubBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CardReaderHubFragment : BaseFragment(R.layout.fragment_card_reader_hub) {
    override fun getFragmentTitle() = resources.getString(R.string.card_reader_onboarding_title)
    val viewModel: CardReaderHubViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCardReaderHubBinding.bind(view)

        initViews(binding)
        observeEvents()
        observeViewState(binding)
    }

    private fun initViews(binding: FragmentCardReaderHubBinding) {
        binding.cardReaderHubRv.layoutManager = LinearLayoutManager(requireContext())
        binding.cardReaderHubRv.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        binding.cardReaderHubRv.adapter = CardReaderHubAdapter()
    }

    private fun observeEvents() {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderDetail -> {
                    findNavController().navigateSafely(
                        CardReaderHubFragmentDirections.actionCardReaderHubFragmentToCardReaderDetailFragment(
                            event.cardReaderFlowParam
                        )
                    )
                }
                is CardReaderHubViewModel.CardReaderHubEvents.NavigateToPurchaseCardReaderFlow -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                is CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderManualsScreen -> {
                    findNavController().navigateSafely(R.id.action_cardReaderHubFragment_to_cardReaderManualsFragment)
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun observeViewState(binding: FragmentCardReaderHubBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { state ->
            (binding.cardReaderHubRv.adapter as CardReaderHubAdapter).setItems(state.rows)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
