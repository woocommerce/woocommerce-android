package com.woocommerce.android.ui.onboarding

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialContainerTransform
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.products.AddProductNavigator
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class StoreOnboardingFragment : BaseFragment() {
    private val viewModel: StoreOnboardingViewModel by viewModels()

    @Inject
    lateinit var addProductNavigator: AddProductNavigator

    private lateinit var rootView: ComposeView

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment_main
            duration = resources.getInteger(R.integer.default_fragment_transition).toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(ContextCompat.getColor(requireContext(), R.color.color_surface))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    StoreOnboardingScreen(viewModel)
                }
            }
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setTransitionName(
            rootView,
            getString(R.string.store_onboarding_full_screen_transition_name)
        )
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is NavigateToLaunchStore ->
                    findNavController().navigateSafely(
                        directions = StoreOnboardingFragmentDirections.actionOnboardingFragmentToLaunchStoreFragment()
                    )

                is NavigateToDomains ->
                    findNavController().navigateSafely(
                        directions = StoreOnboardingFragmentDirections
                            .actionStoreOnboardingFragmentToNavGraphDomainChange()
                    )

                is NavigateToSetupPayments ->
                    findNavController().navigateSafely(
                        directions = StoreOnboardingFragmentDirections
                            .actionStoreOnboardingFragmentToPaymentsPreSetupFragment(taskId = event.taskId)
                    )

                is NavigateToSetupWooPayments ->
                    findNavController().navigateSafely(
                        directions = StoreOnboardingFragmentDirections
                            .actionStoreOnboardingFragmentToWooPaymentsSetupInstructionsFragment()
                    )

                is NavigateToAboutYourStore ->
                    findNavController().navigateSafely(
                        StoreOnboardingFragmentDirections.actionStoreOnboardingFragmentToAboutYourStoreFragment()
                    )

                is NavigateToAddProduct ->
                    with(addProductNavigator) {
                        findNavController().navigateToAddProducts(
                            aiBottomSheetAction = StoreOnboardingFragmentDirections
                                .actionStoreOnboardingFragmentToAddProductWithAIBottomSheet(),
                            typesBottomSheetAction = StoreOnboardingFragmentDirections
                                .actionStoreOnboardingFragmentToProductTypesBottomSheet()
                        )
                    }
            }
        }
    }
}
