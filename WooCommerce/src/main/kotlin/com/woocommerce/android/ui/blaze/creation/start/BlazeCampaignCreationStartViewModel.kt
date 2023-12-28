package com.woocommerce.android.ui.blaze.creation.start

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.blaze.BlazeRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlazeCampaignCreationStartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val blazeRepository: BlazeRepository
) : ScopedViewModel(savedStateHandle) {
    init {
        launch {
            if (blazeRepository.getMostRecentCampaign() != null) {
                triggerEvent(ShowBlazeCampaignCreationIntro)
            } else {
                TODO("Make call to the AI to generate the campaign defaults and then navigate to the AD preview")
            }
        }
    }

    object ShowBlazeCampaignCreationIntro : MultiLiveEvent.Event()
}
