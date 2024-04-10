package com.woocommerce.android.ui.payments.hub

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isInvisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentPaymentsHubBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.list.OrderListViewModel
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingParams
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewModel.PaymentsHubEvents.NavigateToTapTooPaySummaryScreen
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewModel.PaymentsHubEvents.NavigateToTapTooPaySurveyScreen
import com.woocommerce.android.ui.payments.taptopay.summary.TapToPaySummaryFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.UiHelpers
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

private const val APPEARANCE_ANIMATION_DURATION_MS = 600L

@AndroidEntryPoint
class PaymentsHubFragment : BaseFragment(R.layout.fragment_payments_hub) {

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun getFragmentTitle() = resources.getString(R.string.payments_hub_title)
    val viewModel: PaymentsHubViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentPaymentsHubBinding.bind(view)
        setupToolbar(binding)
        initViews(binding)
        observeEvents()
        observeViewState(binding)
    }

    private fun setupToolbar(binding: FragmentPaymentsHubBinding) {
        binding.toolbar.title = resources.getString(R.string.payments_hub_title)
        binding.toolbar.navigationIcon = AppCompatResources.getDrawable(
            requireActivity(),
            R.drawable.ic_back_24dp
        )
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun initViews(binding: FragmentPaymentsHubBinding) {
        binding.paymentsHubRv.layoutManager = LinearLayoutManager(requireContext())
        binding.paymentsHubRv.adapter = PaymentsHubAdapter()
    }

    @Suppress("LongMethod", "ComplexMethod")
    private fun observeEvents() {
        viewModel.event.observe(
            viewLifecycleOwner
        ) { event ->
            when (event) {
                is PaymentsHubViewModel.PaymentsHubEvents.NavigateToCardReaderDetail -> {
                    findNavController().navigateSafely(
                        PaymentsHubFragmentDirections.actionCardReaderHubFragmentToCardReaderDetailFragment(
                            event.cardReaderFlowParam
                        )
                    )
                }
                is PaymentsHubViewModel.PaymentsHubEvents.NavigateToPurchaseCardReaderFlow -> {
                    findNavController().navigate(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                            urlToLoad = event.url,
                            title = resources.getString(event.titleRes)
                        )
                    )
                }
                is PaymentsHubViewModel.PaymentsHubEvents.NavigateToCardReaderManualsScreen -> {
                    findNavController().navigate(
                        PaymentsHubFragmentDirections.actionCardReaderHubFragmentToCardReaderManualsFragment(
                            event.countryConfig
                        )
                    )
                }
                is PaymentsHubViewModel.PaymentsHubEvents.NavigateToCardReaderOnboardingScreen -> {
                    findNavController().navigate(
                        PaymentsHubFragmentDirections.actionCardReaderHubFragmentToCardReaderOnboardingFragment(
                            CardReaderOnboardingParams.Failed(
                                cardReaderFlowParam = CardReaderFlowParam.CardReadersHub(),
                                onboardingState = event.onboardingState
                            ),
                            cardReaderType = null
                        )
                    )
                }
                is PaymentsHubViewModel.PaymentsHubEvents.NavigateToOrderCreationScreen -> {
                    findNavController().navigate(
                        PaymentsHubFragmentDirections.actionCardReaderHubFragmentToOrderListFragment(
                            mode = OrderListViewModel.Mode.START_ORDER_CREATION_WITH_SIMPLE_PAYMENTS_MIGRATION
                        )
                    )
                }
                is PaymentsHubViewModel.PaymentsHubEvents.OpenGenericWebView -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                is PaymentsHubViewModel.PaymentsHubEvents.ShowToastString -> {
                    ToastUtils.showToast(context, event.message)
                }
                is PaymentsHubViewModel.PaymentsHubEvents.ShowToast -> {
                    ToastUtils.showToast(context, getString(event.message))
                }
                is NavigateToTapTooPaySummaryScreen -> {
                    findNavController().navigate(
                        PaymentsHubFragmentDirections.actionCardReaderHubFragmentToTapToPaySummaryFragment(
                            TapToPaySummaryFragment.TestTapToPayFlow.BeforePayment
                        )
                    )
                }
                is NavigateToTapTooPaySurveyScreen -> {
                    NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(SurveyType.PAYMENTS_HUB_TAP_TO_PAY)
                        .apply {
                            findNavController().navigateSafely(this)
                        }
                }
                is MultiLiveEvent.Event.ShowDialog -> {
                    event.showDialog()
                }
                is PaymentsHubViewModel.PaymentsHubEvents.CardReaderUpdateAvailable -> {
                    uiMessageResolver.getInstallSnack(
                        stringResId = event.message,
                        actionListener = event.onClick
                    ).show()
                }
                is PaymentsHubViewModel.PaymentsHubEvents.CardReaderUpdateScreen -> {
                    findNavController().navigate(
                        PaymentsHubFragmentDirections.actionCardReaderHubFragmentToCardReaderUpdateDialogFragment()
                    )
                }
                is PaymentsHubViewModel.PaymentsHubEvents.NavigateToAboutTapToPay -> {
                    findNavController().navigate(
                        PaymentsHubFragmentDirections.actionCardReaderHubFragmentToTapToPayAboutFragment(
                            event.countryConfig
                        )
                    )
                }
                else -> event.isHandled = false
            }
        }
    }

    private fun observeViewState(binding: FragmentPaymentsHubBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { state ->
            with(binding.paymentsHubRv) {
                (adapter as PaymentsHubAdapter).setItems(state.rows)
                updatePadding(
                    bottom = resources.getDimensionPixelSize(
                        if (state.onboardingErrorAction?.text != null) {
                            R.dimen.major_400
                        } else {
                            R.dimen.major_100
                        }
                    )
                )
            }
            binding.paymentsHubLoading.isInvisible = !state.isLoading
            with(binding.paymentsHubOnboardingFailedTv) {
                movementMethod = LinkMovementMethod.getInstance()
                val onboardingErrorAction = state.onboardingErrorAction
                if (onboardingErrorAction != null) {
                    animateErrorAppearance()
                    setOnClickListener { onboardingErrorAction.onClick() }
                }
                UiHelpers.setTextOrHide(this, onboardingErrorAction?.text)
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
