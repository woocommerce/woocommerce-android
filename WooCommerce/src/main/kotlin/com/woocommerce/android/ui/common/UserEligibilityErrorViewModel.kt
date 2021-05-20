package com.woocommerce.android.ui.common

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.User
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class UserEligibilityErrorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefs: AppPrefs,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : ScopedViewModel(savedState) {
    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    final fun start() {
        val email = appPrefs.getUserEmail()
        if (email.isNotEmpty()) {
            val user = userEligibilityFetcher.getUserByEmail(email)
            viewState = viewState.copy(user = user?.toAppModel())
        }
    }

    fun onLogoutButtonClicked() {
        // TODO: will be implemented in another commit
    }

    fun onRetryButtonClicked() {
        // TODO: will be implemented in another commit
    }

    fun onLearnMoreButtonClicked() {
        // TODO: will be implemented in another commit
    }

    @Parcelize
    data class ViewState(
        val user: User? = null
    ) : Parcelable
}
