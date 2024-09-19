package com.woocommerce.android.ui.blaze.detail

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignDetailWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    fun onUrlLoaded(url: String) {
        TODO("Not yet implemented")
    }

    fun onDismiss() {
        TODO("Not yet implemented")
    }


    private val navArgs: BlazeCampaignDetailWebViewFragmentArgs by savedStateHandle.navArgs()
    val urlToLoad = navArgs.urlToLoad
}
