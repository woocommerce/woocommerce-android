package com.woocommerce.android.ui.common

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentUserEligibilityErrorBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.User
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserEligibilityErrorFragment : Fragment(layout.fragment_user_eligibility_error), BackPressListener {
    private val viewModel: UserEligibilityErrorViewModel by viewModels()

    private var _binding: FragmentUserEligibilityErrorBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentUserEligibilityErrorBinding.bind(view)

        (activity as AppCompatActivity).supportActionBar?.hide()
        val toolbar = binding.toolbarLogin.findViewById(R.id.toolbar) as Toolbar
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        setupView()
        setupObservers(viewModel)
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
        binding.buttonHelp.setOnClickListener {
            startActivity(HelpActivity.createIntent(requireActivity(), Origin.USER_ELIGIBILITY_ERROR, null))
        }
    }

    private fun setupObservers(viewModel: UserEligibilityErrorViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.user?.takeIfNotEqualTo(old?.user) { showView(it) }
        }
        viewModel.start()
    }

    private fun showView(user: User) {
        binding.textDisplayname.text = user.getUserNameForDisplay()
        binding.textUserRoles.text = user.roles.joinToString(", ")
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
