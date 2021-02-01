package com.woocommerce.android.ui.common

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentInfoScreenBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.ui.common.InfoScreenFragment.InfoScreenLinkAction.LearnMoreAboutShippingLabels
import com.woocommerce.android.util.ChromeCustomTabUtils
import java.io.Serializable

class InfoScreenFragment : Fragment(R.layout.fragment_info_screen) {
    private val navArgs: InfoScreenFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        val binding = FragmentInfoScreenBinding.bind(view)
        binding.infoHeading.showTextOrHide(navArgs.heading)
        binding.infoMessage.showTextOrHide(navArgs.message)
        binding.infoLink.showTextOrHide(navArgs.linkTitle)

        if (navArgs.imageResource != 0) {
            binding.infoImage.setImageDrawable(ContextCompat.getDrawable(requireContext(), navArgs.imageResource))
        }

        navArgs.linkAction?.let { action ->
            when (action) {
                is LearnMoreAboutShippingLabels -> binding.infoLink.setOnClickListener {
                    ChromeCustomTabUtils.launchUrl(requireContext(), action.LINK)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        AnalyticsTracker.trackViewShown(this)

        activity?.let {
            it.invalidateOptionsMenu()
            if (navArgs.screenTitle != 0) it.title = getString(navArgs.screenTitle)
            (it as? AppCompatActivity)
                ?.supportActionBar
                ?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_24dp)
        }
    }

    private fun TextView.showTextOrHide(@StringRes stringRes: Int) =
        if (stringRes != 0) this.text = getString(stringRes) else hide()

    sealed class InfoScreenLinkAction : Serializable {
        object LearnMoreAboutShippingLabels : InfoScreenLinkAction() {
            const val LINK: String = "https://woocommerce.com/products/shipping/"
        }
    }
}
