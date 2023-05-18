package com.woocommerce.android.ui.prefs.privacy.banner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.CurrentAccount
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyBannerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val currentAccount: CurrentAccount,
) : ScopedViewModel(savedStateHandle) {

    private val _analyticsEnabled = MutableLiveData(false)

    val analyticsEnabled: LiveData<Boolean> = _analyticsEnabled

    init {
        launch {
            currentAccount.observe().collect {
                _analyticsEnabled.value = it?.tracksOptOut == false
            }
        }
    }
}
