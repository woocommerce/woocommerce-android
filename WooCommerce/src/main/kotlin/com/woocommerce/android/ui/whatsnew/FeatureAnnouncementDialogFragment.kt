package com.woocommerce.android.ui.whatsnew

import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FeatureAnnouncementDialogFragmentBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import org.wordpress.android.util.DisplayUtils

class FeatureAnnouncementDialogFragment : DialogFragment() {
    companion object {
        const val TAG: String = "FeatureAnnouncementDialog"
        const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.25f
        const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
    }

    private val viewModel: FeatureAnnouncementViewModel by viewModels()
    private lateinit var listAdapter: FeatureAnnouncementListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.feature_announcement_dialog_fragment, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isTabletLandscape()) {
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
        } else {
            /* This draws the dialog as full screen */
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FeatureAnnouncementDialogFragmentBinding.bind(view)

        setupView(binding)
        setupObservers()
    }

    override fun onStart() {
        super.onStart()
        if (isTabletLandscape()) {
            dialog?.window?.setLayout(
                (DisplayUtils.getDisplayPixelWidth() * TABLET_LANDSCAPE_WIDTH_RATIO).toInt(),
                (DisplayUtils.getDisplayPixelHeight(context) * TABLET_LANDSCAPE_HEIGHT_RATIO).toInt()
            )
        }
    }

    private fun isTabletLandscape(): Boolean {
        val isTablet = DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)
        val isLandscape = DisplayUtils.isLandscape(context)

        return isTablet && isLandscape
    }

    private fun setupView(binding: FeatureAnnouncementDialogFragmentBinding) {
        binding.closeFeatureAnnouncementButton.setOnClickListener {
            findNavController().popBackStack()
        }

        listAdapter = FeatureAnnouncementListAdapter()
        binding.featureList.adapter = listAdapter
        binding.featureList.layoutManager = LinearLayoutManager(activity)
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.announcement.takeIfNotEqualTo(old?.announcement) {
                it?.let {
                    listAdapter.updateData(it.features)
                }
            }
        }
    }
}
