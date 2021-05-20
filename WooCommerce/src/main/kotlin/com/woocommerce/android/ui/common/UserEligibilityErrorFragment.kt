package com.woocommerce.android.ui.common

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentUserEligibilityErrorBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.User
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class UserEligibilityErrorFragment : Fragment(layout.fragment_user_eligibility_error), BackPressListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: UserEligibilityErrorViewModel by viewModels()

    private var _binding: FragmentUserEligibilityErrorBinding? = null
    private val binding get() = _binding!!

    private var progressDialog: CustomProgressDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserEligibilityErrorBinding.bind(view)

        setHasOptionsMenu(true)
        setupView()
        setupObservers(viewModel)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_login, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help) {
            startActivity(HelpActivity.createIntent(requireActivity(), Origin.USER_ELIGIBILITY_ERROR, null))
            return true
        }

        return false
    }

    private fun setupView() {
        val btnBinding = binding.epilogueButtonBar
        with(btnBinding.buttonPrimary) {
            visibility = View.VISIBLE
            text = getString(R.string.retry)
            setOnClickListener { viewModel.onRetryButtonClicked() }
        }

        with(btnBinding.buttonSecondary) {
            visibility = View.VISIBLE
            text = getString(R.string.login_try_another_account)
            setOnClickListener { viewModel.onLogoutButtonClicked() }
        }

        binding.btnSecondaryAction.setOnClickListener { viewModel.onLearnMoreButtonClicked() }
    }

    private fun setupObservers(viewModel: UserEligibilityErrorViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.user?.takeIfNotEqualTo(old?.user) { showView(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
        }
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> {
                    uiMessageResolver.showSnack(event.message)
                }
                is Exit -> {
                    findNavController().navigateUp()
                }
                else -> event.isHandled = false
            }
        })
        viewModel.start()
    }

    private fun showView(user: User) {
        binding.textDisplayname.text = user.getUserNameForDisplay()
        binding.textUserRoles.text = user.roles.joinToString(", ")
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(R.string.user_access_verifying),
                getString(R.string.web_view_loading_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestAllowBackPress(): Boolean {
        activity?.finish()
        return false
    }
}
