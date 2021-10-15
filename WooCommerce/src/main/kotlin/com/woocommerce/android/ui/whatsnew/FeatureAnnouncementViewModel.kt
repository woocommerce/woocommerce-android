package com.woocommerce.android.ui.whatsnew

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class FeatureAnnouncementViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val prefs: AppPrefs,
    private val buildConfigWrapper: BuildConfigWrapper,
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, FeatureAnnouncementViewState())
    private var viewState by viewStateData

    fun setAnnouncementData(announcement: FeatureAnnouncement) {
        viewState = viewState.copy(announcement = announcement)
    }

    fun handleAnnouncementIsViewed() {
        prefs.setLastVersionWithAnnouncement(buildConfigWrapper.versionName)
    }

    @Parcelize
    data class FeatureAnnouncementViewState(
        val announcement: FeatureAnnouncement? = null
    ) : Parcelable
}
