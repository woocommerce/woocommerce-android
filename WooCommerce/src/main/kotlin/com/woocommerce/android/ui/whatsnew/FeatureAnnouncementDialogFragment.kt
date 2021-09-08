package com.woocommerce.android.ui.whatsnew

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FeatureAnnouncementDialogFragmentBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo

class FeatureAnnouncementDialogFragment : DialogFragment() {
    companion object {
        const val TAG: String = "FeatureAnnouncementDialog"
    }

    private val viewModel: FeatureAnnouncementViewModel by viewModels()
    private lateinit var listAdapter: FeatureAnnouncementListAdapter

    override fun getTheme(): Int {
        return R.style.Theme_Woo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.feature_announcement_dialog_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FeatureAnnouncementDialogFragmentBinding.bind(view)

        setupView(binding)
        setupObservers()
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
