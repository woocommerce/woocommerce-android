package com.woocommerce.android.ui.whatsnew

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class FeatureAnnouncementViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, FeatureAnnouncementViewState())
    private var viewState by viewStateData

    init {
        viewState = viewState.copy(
            announcement = FeatureAnnouncementRepository.exampleAnnouncement
        )
    }

    @Parcelize
    data class FeatureAnnouncementViewState(
        val announcement: FeatureAnnouncement? = null
    ) : Parcelable
}
