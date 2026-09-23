package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.AppPrefsWrapper
import javax.inject.Inject

class WooPosHasConnectedReaderBefore @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    operator fun invoke(): Boolean =
        appPrefsWrapper.getLastConnectedCardReaderId() != null ||
            appPrefsWrapper.getLastConnectedPhoneDeviceId() != null
}
