package com.woocommerce.android.tools

import org.wordpress.android.mediapicker.api.MediaInsertHandler
import org.wordpress.android.mediapicker.api.MediaInsertHandlerFactory
import org.wordpress.android.mediapicker.api.MediaInsertUseCase
import org.wordpress.android.mediapicker.api.MediaPickerSetup
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.*
import org.wordpress.android.mediapicker.source.device.DeviceMediaInsertUseCase.DeviceMediaInsertUseCaseFactory
import javax.inject.Inject

class WooMediaInsertHandlerFactory @Inject constructor(
    private val deviceMediaInsertUseCaseFactory: DeviceMediaInsertUseCaseFactory,
) : MediaInsertHandlerFactory {
    override fun build(mediaPickerSetup: MediaPickerSetup): MediaInsertHandler {
        return when (mediaPickerSetup.primaryDataSource) {
            DEVICE, CAMERA, SYSTEM_PICKER -> deviceMediaInsertUseCaseFactory.build(mediaPickerSetup.areResultsQueued)
            GIF_LIBRARY -> DefaultMediaInsertUseCase
        }.toMediaInsertHandler()
    }

    private fun MediaInsertUseCase.toMediaInsertHandler() = MediaInsertHandler(this)

    private object DefaultMediaInsertUseCase : MediaInsertUseCase
}
