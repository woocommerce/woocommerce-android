package com.woocommerce.android.ui.whatsnew

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FeatureAnnouncementDialogFragmentBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FeatureAnnouncementDialogFragment : DialogFragment() {
    @Inject lateinit var sizeSetup: FeatureAnnouncementSizeSetupHelper

    private val viewModel: FeatureAnnouncementViewModel by viewModels()
    private val navArgs: FeatureAnnouncementDialogFragmentArgs by navArgs()
    private lateinit var listAdapter: FeatureAnnouncementListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.feature_announcement_dialog_fragment, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycle.addObserver(sizeSetup)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FeatureAnnouncementDialogFragmentBinding.bind(view)

        viewModel.setAnnouncementData(navArgs.announcement)
        setupView(binding)
        setupObservers(binding)
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, false)
        }
    }

    private fun setupView(binding: FeatureAnnouncementDialogFragmentBinding) {
        listAdapter = FeatureAnnouncementListAdapter()
        binding.featureList.adapter = listAdapter
        binding.featureList.layoutManager = LinearLayoutManager(activity)
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.handleAnnouncementIsViewed()

        super.onDismiss(dialog)
    }

    private fun setupObservers(binding: FeatureAnnouncementDialogFragmentBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.announcement.takeIfNotEqualTo(old?.announcement) {
                it?.let { announcement ->
                    listAdapter.submitList(announcement.features)
                    if (announcement.detailsUrl.isNotEmpty()) {
                        binding.closeFeatureAnnouncementButton.text = getString(R.string.learn_more)
                        binding.closeFeatureAnnouncementButton.setOnClickListener {
                            openDetailsUrl(announcement.detailsUrl)
                            findNavController().popBackStack()
                        }
                        binding.dismissFeatureAnnouncementButton.visibility = View.VISIBLE
                        binding.dismissFeatureAnnouncementButton.setOnClickListener {
                            findNavController().popBackStack()
                        }
                    } else {
                        binding.dismissFeatureAnnouncementButton.visibility = View.GONE
                        binding.dismissFeatureAnnouncementButton.setOnClickListener(null)
                        binding.closeFeatureAnnouncementButton.text = getString(R.string.continue_button)
                        binding.closeFeatureAnnouncementButton.setOnClickListener {
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    private fun openDetailsUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
            startActivity(intent)
        }.onFailure {
            WooLog.e(WooLog.T.UTILS, "Failed to open URL: $url", it)
        }
    }
}
