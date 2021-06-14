package com.woocommerce.android.util.crashlogging

import java.util.UUID
import javax.inject.Inject

class UuidGenerator @Inject constructor() {
    fun generateUuid(): String = UUID.randomUUID().toString()
}
