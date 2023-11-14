package com.woocommerce.android.ui.login.storecreation.name

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.CheckNotificationsPermission
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.NavigateToStoreProfiler
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.RequestNotificationsPermission
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoreNamePickerFragment : BaseFragment() {
    private val viewModel: StoreNamePickerViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val requestPermissionLauncher = registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        (viewModel.event.value as? RequestNotificationsPermission)?.onPermissionsRequestResult?.invoke(isGranted)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    StoreNamePickerScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is MultiLiveEvent.Event.NavigateToHelpScreen -> navigateToHelpScreen(event.origin)
                is NavigateToStoreProfiler -> navigateToStoreProfiler()
                is RequestNotificationsPermission -> {
                    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                        WooPermissionUtils.requestNotificationsPermission(requestPermissionLauncher)
                    }
                }

                is CheckNotificationsPermission -> {
                    if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                        event.onPermissionsCheckResult(
                            WooPermissionUtils.hasNotificationsPermission(requireContext()),
                            WooPermissionUtils.shouldShowNotificationsRationale(requireActivity())
                        )
                    }
                }
            }
        }
    }

    private fun navigateToStoreProfiler() {
        StoreNamePickerFragmentDirections.actionStoreNamePickerFragmentToStoreProfilerCategoryFragment()
            .let { findNavController().navigateSafely(it) }
    }
}
