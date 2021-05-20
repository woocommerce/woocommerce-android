package com.woocommerce.android.ui.common

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.User
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCUserStore
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class UserEligibilityErrorViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val appPrefs: AppPrefs,
    private val userStore: WCUserStore,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {
    final val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    final fun start() {
        val email = appPrefs.getUserEmail()
        if (email.isNotEmpty()) {
            val user = userStore.getUserByEmail(selectedSite.get(), email)
            viewState = viewState.copy(user = user?.toAppModel())
        }
    }

    @Parcelize
    data class ViewState(
        val user: User? = null
    ) : Parcelable
}
