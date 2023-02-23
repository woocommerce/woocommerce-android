package com.woocommerce.android.ui.login.jetpack.wpcom

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.jetpack.wpcom.JetpackActivationWPComEmailViewModel.ShowMagicLinkScreen
import com.woocommerce.android.ui.login.jetpack.wpcom.JetpackActivationWPComEmailViewModel.ShowPasswordScreen
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class JetpackActivationWPComEmailFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: JetpackActivationWPComEmailViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    JetpackActivationWPComScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowPasswordScreen -> {
                    // TODO
                    Toast.makeText(requireContext(), "$event", Toast.LENGTH_SHORT).show()
                }
                is ShowMagicLinkScreen -> {
                    // TODO
                    Toast.makeText(requireContext(), "$event", Toast.LENGTH_SHORT).show()
                }
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                Exit -> findNavController().navigateUp()
            }
        }
    }
}
