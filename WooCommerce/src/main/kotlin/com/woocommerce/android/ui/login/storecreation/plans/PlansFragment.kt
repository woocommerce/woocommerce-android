package com.woocommerce.android.ui.login.storecreation.plans

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.plans.PlansViewModel.NavigateToNextStep
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@AndroidEntryPoint
class PlansFragment : BaseFragment() {
    private val viewModel: PlansViewModel by viewModels()

    @Inject internal lateinit var authenticator: WPComWebViewAuthenticator
    @Inject internal lateinit var userAgent: UserAgent

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    PlanScreen(viewModel = viewModel, authenticator, userAgent)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        viewModel.setIAPActivityWrapper(IAPActivityWrapper(requireActivity() as AppCompatActivity))
    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel)
        super.onDestroy()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is NavigateToNextStep -> navigateToInstallationFragment()
            }
        }
    }

    private fun navigateToInstallationFragment() {
        findNavController().navigateSafely(
            PlansFragmentDirections.actionPlansFragmentToInstallationFragment()
        )
    }
}
