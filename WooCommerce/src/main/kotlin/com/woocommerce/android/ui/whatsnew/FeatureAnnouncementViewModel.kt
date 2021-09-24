package com.woocommerce.android.ui.whatsnew

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class FeatureAnnouncementViewModel @Inject constructor(
    savedState: SavedStateHandle,
    featureAnnouncementRepository: FeatureAnnouncementRepository
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, FeatureAnnouncementViewState())
    private var viewState by viewStateData

    init {
        launch {
            val latestAnnouncement = featureAnnouncementRepository.getLatestFeatureAnnouncement(true)
                ?: featureAnnouncementRepository.getLatestFeatureAnnouncement(false)

            latestAnnouncement?.let {
                viewState = viewState.copy(
                    announcement = it
                )

                // TODO for display logic:
                // 1. save latest announcement's announcement version in prefs
                // 2. save current app version in prefs
            }
        }
    }

    @Parcelize
    data class FeatureAnnouncementViewState(
        val announcement: FeatureAnnouncement? = null
    ) : Parcelable
}
