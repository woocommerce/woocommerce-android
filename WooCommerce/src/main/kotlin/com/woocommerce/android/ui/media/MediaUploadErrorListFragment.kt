package com.woocommerce.android.ui.media

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentMediaUploadErrorListBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaUploadErrorListFragment : BaseFragment(R.layout.fragment_media_upload_error_list) {
    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

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

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_downloadable_files_upload_failed),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
            }
        )
    }

    private fun setupObservers(viewModel: MediaUploadErrorListViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.uploadErrorList.takeIfNotEqualTo(old?.uploadErrorList) { errors ->
                mediaUploadErrorListAdapter.mediaErrorList = errors
            }
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
