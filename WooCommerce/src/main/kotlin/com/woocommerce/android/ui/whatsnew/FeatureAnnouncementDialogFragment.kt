package com.woocommerce.android.ui.whatsnew

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FeatureAnnouncementDialogFragmentBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FeatureAnnouncementDialogFragment : DialogFragment() {
    @Inject lateinit var displayAsDialog: FeatureAnnouncementDisplayAsDialog

    private val viewModel: FeatureAnnouncementViewModel by viewModels()
    private val navArgs: FeatureAnnouncementDialogFragmentArgs by navArgs()
    private lateinit var listAdapter: FeatureAnnouncementListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.feature_announcement_dialog_fragment, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (displayAsDialog()) {
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog)
        } else {
            /* This draws the dialog as full screen */
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FeatureAnnouncementDialogFragmentBinding.bind(view)

        viewModel.setAnnouncementData(navArgs.announcement)
        setupView(binding)
        setupObservers()
    }

    private fun setupView(binding: FeatureAnnouncementDialogFragmentBinding) {
        binding.closeFeatureAnnouncementButton.setOnClickListener {
            viewModel.handleAnnouncementIsViewed()
            findNavController().popBackStack()
        }

        listAdapter = FeatureAnnouncementListAdapter()
        binding.featureList.adapter = listAdapter
        binding.featureList.layoutManager = LinearLayoutManager(activity)
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.handleAnnouncementIsViewed()

        super.onDismiss(dialog)
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.announcement.takeIfNotEqualTo(old?.announcement) {
                it?.let {
                    listAdapter.submitList(it.features)
                }
            }
        }
    }
}
