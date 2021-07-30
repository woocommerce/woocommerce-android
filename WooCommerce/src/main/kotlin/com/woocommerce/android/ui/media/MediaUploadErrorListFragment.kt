package com.woocommerce.android.ui.media

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentMediaUploadErrorListBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaUploadErrorListFragment : BaseFragment(R.layout.fragment_media_upload_error_list) {
    private val mediaUploadErrorListAdapter: MediaUploadErrorListAdapter by lazy {
        MediaUploadErrorListAdapter()
    }

    private val viewModel: MediaUploadErrorListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentMediaUploadErrorListBinding.bind(view)

        with(binding.mediaUploadErrorList) {
            adapter = mediaUploadErrorListAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        setupObservers(viewModel)
    }

    private fun setupObservers(viewModel: MediaUploadErrorListViewModel) {
        viewModel.mediaUploadErrorList.observe(
            viewLifecycleOwner,
            Observer {
                mediaUploadErrorListAdapter.mediaErrorList = it
            }
        )
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.toolBarTitle.takeIfNotEqualTo(old?.toolBarTitle) {
                activity?.title = it
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }
}
