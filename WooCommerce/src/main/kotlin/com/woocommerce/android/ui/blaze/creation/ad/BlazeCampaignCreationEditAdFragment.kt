package com.woocommerce.android.ui.blaze.creation.ad

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.mediapicker.MediaPickerHelper
import com.woocommerce.android.mediapicker.MediaPickerHelper.MediaPickerResultHandler
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.blaze.creation.ad.BlazeCampaignCreationEditAdViewModel.EditAdResult
import com.woocommerce.android.ui.blaze.creation.ad.BlazeCampaignCreationEditAdViewModel.ShowMediaLibrary
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BlazeCampaignCreationEditAdFragment : BaseFragment(), MediaPickerResultHandler {
    companion object {
        const val EDIT_AD_RESULT = "edit_ad_result"
    }

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    val viewModel: BlazeCampaignCreationEditAdViewModel by viewModels()

    @Inject
    lateinit var mediaPickerHelper: MediaPickerHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return composeView {
            BlazeCampaignCreationPreviewScreen(viewModel = viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().popBackStack()
                is ShowMediaLibrary -> mediaPickerHelper.showMediaPicker(event.source)
                is ShowDialog -> event.showDialog()
                is ExitWithResult<*> -> {
                    navigateBackWithResult(EDIT_AD_RESULT, event.data as EditAdResult)
                }
            }
        }
    }

    override fun onDeviceMediaSelected(imageUris: List<Uri>, source: String) {
        if (imageUris.isNotEmpty()) {
            viewModel.onLocalImageSelected(imageUris.first().toString())
        }
    }

    override fun onWPMediaSelected(images: List<Image>) {
        if (images.isNotEmpty()) {
            viewModel.onWPMediaSelected(images.first())
        }
    }
}
