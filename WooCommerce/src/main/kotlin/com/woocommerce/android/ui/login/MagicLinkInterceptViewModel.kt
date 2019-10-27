package com.woocommerce.android.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.ui.reviews.RequestResult.ERROR
import com.woocommerce.android.ui.reviews.RequestResult.NO_ACTION_NEEDED
import com.woocommerce.android.ui.reviews.RequestResult.SUCCESS
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class MagicLinkInterceptViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val magicLinkInterceptRepository: MagicLinkInterceptRepository
) : ScopedViewModel(mainDispatcher) {
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isAuthTokenUpdated = MutableLiveData<Boolean>()
    val isAuthTokenUpdated: LiveData<Boolean> = _isAuthTokenUpdated

    fun updateMagicLinkAuthToken(authToken: String) {
        launch {
            _isLoading.value = true

            when (magicLinkInterceptRepository.updateMagicLinkAuthToken(authToken)) {
                SUCCESS -> _isAuthTokenUpdated.value = true

                // Errors can occur if the auth token was not updated to the FluxC cache
                // or if the user is not logged in
                ERROR -> _isAuthTokenUpdated.value = false
                NO_ACTION_NEEDED -> { }
            }
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        magicLinkInterceptRepository.onCleanup()
    }
}
