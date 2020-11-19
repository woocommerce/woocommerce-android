package com.woocommerce.android.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.navigation.fragment.navArgs
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.extensions.hide
import kotlinx.android.synthetic.main.fragment_info_screen.*

class InfoScreenFragment : Fragment() {
    private val navArgs: InfoScreenFragmentArgs by navArgs()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_info_screen, container, false)
    }

    override fun onResume() {
        super.onResume()

        activity?.let {
            it.invalidateOptionsMenu()
            if (navArgs.screenTitle != 0) it.title = getString(navArgs.screenTitle)
            (it as? AppCompatActivity)
                ?.supportActionBar
                ?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_white_24dp)
        }

        showTextOrHide(navArgs.heading, info_heading)
        showTextOrHide(navArgs.message, info_message)
        showTextOrHide(navArgs.linkTitle, info_link)

        if (navArgs.imageResource != 0) {
            info_image.setImageDrawable(ContextCompat.getDrawable(requireContext(), navArgs.imageResource))
        }
    }

    private fun showTextOrHide(@StringRes stringRes: Int, view: TextView) =
        if (stringRes != 0) view.text = getString(stringRes) else view.hide()
}
