package com.woocommerce.android.ui.mystore

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.navigation.NavHostController
import com.woocommerce.android.ui.login.LoginRepository
import com.woocommerce.commons.viewmodel.ScopedViewModel
import com.woocommerce.commons.viewmodel.getStateFlow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.SiteModel

@Suppress("UnusedPrivateProperty")
@HiltViewModel(assistedFactory = MyStoreViewModel.Factory::class)
class MyStoreViewModel @AssistedInject constructor(
    private val loginRepository: LoginRepository,
    @Assisted private val navController: NavHostController,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = this,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    init { observeLoginChanges() }

    private fun observeLoginChanges() {
        loginRepository.currentSite
            .onEach { updateSiteData(it) }
            .launchIn(this)
    }

    private fun updateSiteData(site: SiteModel) {
        _viewState.update {
            it.copy(
                currentSiteId = site.siteId.toString(),
                currentSiteName = site.name
            )
        }
    }

    @Parcelize
    data class ViewState(
        val currentSiteId: String? = null,
        val currentSiteName: String? = null
    ) : Parcelable

    @AssistedFactory
    interface Factory {
        fun create(navController: NavHostController): MyStoreViewModel
    }
}
