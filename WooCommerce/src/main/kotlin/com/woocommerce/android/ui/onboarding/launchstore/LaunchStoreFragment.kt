package com.woocommerce.android.ui.onboarding.launchstore

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.onboarding.launchstore.LaunchStoreViewModel.ShareStoreUrl
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.LogLevel.e
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ToastUtils

@AndroidEntryPoint
class LaunchStoreFragment : BaseFragment() {
    private val viewModel: LaunchStoreViewModel by viewModels()
    private lateinit var rootView: ComposeView

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    LaunchStoreScreen(viewModel = viewModel)
                }
            }
        }
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onDestroy() {
        lifecycle.removeObserver(viewModel)
        super.onDestroy()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is ShareStoreUrl -> shareStoreUrl(event.url)
                is ShowDialog -> event.showDialog()
            }
        }
    }

    private fun shareStoreUrl(storeUrl: String) {
        val title = getString(R.string.store_onboarding_launch_store_share_url_button)
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, storeUrl)
            putExtra(Intent.EXTRA_SUBJECT, title)
            type = "text/plain"
        }
        kotlin.runCatching {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
        }.fold(
            onSuccess = {},
            onFailure = {
                WooLog.e(T.UTILS, "Exception trying to share store url. Exception: $e")
                ToastUtils.showToast(requireContext(), R.string.store_onboarding_share_url_error)
            }
        )
    }
}
