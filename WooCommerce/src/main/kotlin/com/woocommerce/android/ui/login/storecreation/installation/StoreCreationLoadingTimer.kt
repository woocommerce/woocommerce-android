package com.woocommerce.android.ui.login.storecreation.installation

import android.os.CountDownTimer
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.login.storecreation.installation.StoreInstallationViewModel.ViewState.StoreCreationLoadingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class StoreCreationLoadingTimer @Inject constructor() {
    private companion object {
        private const val LOADING_TOTAL_TIME = 60000L
        const val DELAY_BETWEEN_PROGRESS_UPDATES = 200L
    }

    private var progress = 0F
    private val loadingState: MutableStateFlow<StoreCreationLoadingState> = MutableStateFlow(
        StoreCreationLoadingState(
            progress = progress,
            title = string.store_creation_in_progress_title_1,
            description = string.store_creation_in_progress_description_1
        )
    )

    private val timer = object : CountDownTimer(
        LOADING_TOTAL_TIME,
        DELAY_BETWEEN_PROGRESS_UPDATES
    ) {
        @Suppress("MagicNumber")
        override fun onTick(millisUntilFinished: Long) {
            progress += 0.003F
            val updatedState = when {
                millisUntilFinished > 50000 ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_1,
                        description = string.store_creation_in_progress_description_1
                    )

                millisUntilFinished > 40000 ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_2,
                        description = string.store_creation_in_progress_description_2
                    )

                millisUntilFinished > 30000 ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_3,
                        description = string.store_creation_in_progress_description_3
                    )

                millisUntilFinished > 20000 ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_4,
                        description = string.store_creation_in_progress_description_4
                    )

                millisUntilFinished > 10000 ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_5,
                        description = string.store_creation_in_progress_description_5
                    )

                else ->
                    StoreCreationLoadingState(
                        progress = progress,
                        title = string.store_creation_in_progress_title_6,
                        description = string.store_creation_in_progress_description_6
                    )
            }
            loadingState.value = updatedState
        }

        override fun onFinish() = Unit
    }

    fun observe(): Flow<StoreCreationLoadingState> = loadingState

    fun startTimer() {
        timer.start()
    }

    fun cancelTimer() {
        timer.cancel()
    }
}
