package com.woocommerce.android.ui.payments.cardreader.hub

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentCardReaderHubBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.NavigateToTapTooPaySummaryScreen
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubEvents.NavigateToTapTooPaySurveyScreen
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingParams
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UiHelpers
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils

private const val APPEARANCE_ANIMATION_DURATION_MS = 600L

@AndroidEntryPoint
class CardReaderHubFragment : BaseFragment(R.layout.fragment_card_reader_hub) {
    override fun getFragmentTitle() = resources.getString(R.string.payments_hub_title)
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
        binding.cardReaderHubRv.adapter = CardReaderHubAdapter()
    }

    @Suppress("LongMethod")
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
                    findNavController().navigate(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                            urlToLoad = event.url,
                            title = resources.getString(event.titleRes)
                        )
                    )
                }
                is CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderManualsScreen -> {
                    findNavController().navigate(
                        CardReaderHubFragmentDirections.actionCardReaderHubFragmentToCardReaderManualsFragment(
                            event.countryConfig
                        )
                    )
                }
                is CardReaderHubViewModel.CardReaderHubEvents.NavigateToCardReaderOnboardingScreen -> {
                    findNavController().navigate(
                        CardReaderHubFragmentDirections.actionCardReaderHubFragmentToCardReaderOnboardingFragment(
                            CardReaderOnboardingParams.Failed(
                                cardReaderFlowParam = CardReaderFlowParam.CardReadersHub(),
                                onboardingState = event.onboardingState
                            ),
                            cardReaderType = null
                        )
                    )
                }
                is CardReaderHubViewModel.CardReaderHubEvents.NavigateToPaymentCollectionScreen -> {
                    findNavController().navigate(
                        CardReaderHubFragmentDirections.actionCardReaderHubFragmentToSimplePayments()
                    )
                }
                is CardReaderHubViewModel.CardReaderHubEvents.OpenGenericWebView -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                is CardReaderHubViewModel.CardReaderHubEvents.ShowToastString -> {
                    ToastUtils.showToast(context, event.message)
                }
                is CardReaderHubViewModel.CardReaderHubEvents.ShowToast -> {
                    ToastUtils.showToast(context, getString(event.message))
                }
                is NavigateToTapTooPaySummaryScreen -> {
                    findNavController().navigate(
                        CardReaderHubFragmentDirections.actionCardReaderHubFragmentToTapToPaySummaryFragment()
                    )
                }
                is NavigateToTapTooPaySurveyScreen -> {
                    NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(SurveyType.PAYMENTS_HUB_TAP_TO_PAY)
                        .apply {
                            findNavController().navigateSafely(this)
                        }
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun observeViewState(binding: FragmentCardReaderHubBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { state ->
            (binding.cardReaderHubRv.adapter as CardReaderHubAdapter).setItems(state.rows)
            binding.cardReaderHubLoading.isInvisible = !state.isLoading
            with(binding.cardReaderHubOnboardingFailedTv) {
                movementMethod = LinkMovementMethod.getInstance()
                val onboardingErrorAction = state.onboardingErrorAction
                if (onboardingErrorAction != null) {
                    animateErrorAppearance()
                    setOnClickListener { onboardingErrorAction.onClick() }
                }
                UiHelpers.setTextOrHide(this, onboardingErrorAction?.text)
            }
            with(binding.learnMoreIppTv) {
                learnMore.setOnClickListener { state.learnMoreIppState?.onClick?.invoke() }
                UiHelpers.setTextOrHide(binding.learnMoreIppTv.learnMore, state.learnMoreIppState?.label)
            }
        }
    }

    private fun MaterialTextView.animateErrorAppearance() {
        val slide = Slide(Gravity.BOTTOM).apply {
            duration = APPEARANCE_ANIMATION_DURATION_MS
            addTarget(this@animateErrorAppearance)
        }
        TransitionManager.beginDelayedTransition(parent as ViewGroup, slide)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        viewModel.onViewVisible()
    }
}
