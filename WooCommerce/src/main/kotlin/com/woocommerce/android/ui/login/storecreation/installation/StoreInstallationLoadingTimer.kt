package com.woocommerce.android.ui.login.storecreation.installation

import android.os.CountDownTimer
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.StoreCreationLoadingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class StoreInstallationLoadingTimer @Inject constructor() {
    private companion object {
        private const val LOADING_TOTAL_TIME = 40000L
        const val DELAY_BETWEEN_PROGRESS_UPDATES = 200L
    }

    private var progress = 0.6F
    private val loadingState: MutableStateFlow<StoreCreationLoadingState> = MutableStateFlow(
        StoreCreationLoadingState(
            progress = progress,
            title = string.store_creation_in_progress_title_1,
            description = string.store_creation_in_progress_description_1,
            image = R.drawable.store_creation_loading_almost_there
        )
    )

    private var storeLoadingCountDownTimer = buildTimer()

    fun observe(): Flow<StoreCreationLoadingState> = loadingState

    fun startTimer() {
        storeLoadingCountDownTimer.start()
    }

    fun resetTimer() {
        storeLoadingCountDownTimer.cancel()
        storeLoadingCountDownTimer = buildTimer()
        progress = 0F
    }

    private fun buildTimer() = object : CountDownTimer(
        LOADING_TOTAL_TIME,
        DELAY_BETWEEN_PROGRESS_UPDATES
    ) {
        @Suppress("MagicNumber")
        override fun onTick(millisUntilFinished: Long) {
            progress += 0.003F
            val updatedState = when {
                millisUntilFinished > 30000 ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_1,
                        description = string.store_creation_in_progress_description_1,
                        image = R.drawable.store_creation_loading_almost_there
                    )

                millisUntilFinished > 20000 ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_2,
                        description = string.store_creation_in_progress_description_2,
                        image = R.drawable.store_creation_loading_extending_capabilities
                    )

                millisUntilFinished > 10000 ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_3,
                        description = string.store_creation_in_progress_description_3,
                        image = R.drawable.store_creation_loading_turning_on_lights
                    )

                else ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_4,
                        description = string.store_creation_in_progress_description_4,
                        image = R.drawable.store_creation_loading_opening_doorsdark
                    )
            }
            loadingState.value = updatedState
        }

        override fun onFinish() = Unit
    }
}
