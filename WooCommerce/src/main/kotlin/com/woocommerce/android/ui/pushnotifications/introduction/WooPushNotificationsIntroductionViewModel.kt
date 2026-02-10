package com.woocommerce.android.ui.pushnotifications.introduction

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppUrls
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class WooPushNotificationsIntroductionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    fun onContinueClick() {
        // TODO Implement connection flow navigation when available
    }

    fun onNotNowClick() {
        triggerEvent(Exit)
    }

    fun onWhatIsWPComClick() {
        triggerEvent(OpenUrlEvent(AppUrls.LOGIN_WITH_EMAIL_WHAT_IS_WORDPRESS_COM_ACCOUNT))
    }

    data class OpenUrlEvent(val url: String) : Event()
}
