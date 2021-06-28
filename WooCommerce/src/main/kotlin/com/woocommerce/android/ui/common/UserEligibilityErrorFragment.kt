package com.woocommerce.android.ui.common

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentUserEligibilityErrorBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.User
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Logout
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class UserEligibilityErrorFragment : BaseFragment(layout.fragment_user_eligibility_error), BackPressListener {
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
            startActivity(
                HelpActivity.createIntent(
                    requireActivity(), Origin.USER_ELIGIBILITY_ERROR, arrayListOf(binding.textUserRoles.text.toString())
                )
            )
            return true
        }

        return false
    }

    private fun setupView() {
        // hide images in landscape unless this device is a tablet
        val isLandscape = DisplayUtils.isLandscape(context)
        val hideImages = isLandscape &&
            !DisplayUtils.isTablet(context) &&
            !DisplayUtils.isXLargeTablet(context)
        binding.imageView2.isVisible = !hideImages

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

        binding.btnSecondaryAction.setOnClickListener {
            ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.WOOCOMMERCE_USER_ROLES)
        }
    }

    private fun setupObservers(viewModel: UserEligibilityErrorViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.user?.takeIfNotEqualTo(old?.user) { showView(it) }
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) { showProgressDialog(it) }
        }
        viewModel.event.observe(
            viewLifecycleOwner,
            Observer { event ->
                when (event) {
                    is ShowSnackbar -> {
                        uiMessageResolver.showSnack(event.message)
                    }
                    is Exit -> {
                        findNavController().navigateUp()
                    }
                    is Logout -> {
                        requireActivity().apply {
                            setResult(Activity.RESULT_CANCELED)
                            val intent = Intent(activity, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            LoginMode.WOO_LOGIN_MODE.putInto(intent)
                            startActivity(intent)
                            finish()
                        }
                    }
                    else -> event.isHandled = false
                }
            }
        )
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

    override fun getFragmentTitle() = ""

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
