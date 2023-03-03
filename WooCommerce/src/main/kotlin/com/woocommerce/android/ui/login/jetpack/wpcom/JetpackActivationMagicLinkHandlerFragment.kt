package com.woocommerce.android.ui.login.jetpack.wpcom

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.login.jetpack.GoToStore
import com.woocommerce.android.ui.login.jetpack.wpcom.JetpackActivationWPComPostLoginViewModel.ShowJetpackActivationScreen
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class JetpackActivationMagicLinkHandlerFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private val viewModel: JetpackActivationMagicLinkHandlerViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return View(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ShowJetpackActivationScreen -> navigateToJetpackActivationScreen(event)
                is GoToStore -> goToStore()
            }
        }
    }

    private fun navigateToJetpackActivationScreen(event: ShowJetpackActivationScreen) {
        findNavController().navigateSafely(
            JetpackActivationMagicLinkHandlerFragmentDirections
                .actionJetpackActivationMagicLinkHandlerFragmentToJetpackActivationMainFragment(
                    isJetpackInstalled = event.isJetpackInstalled,
                    siteUrl = event.siteUrl
                )
        )
    }

    private fun goToStore() {
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }
}
