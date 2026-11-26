package com.woocommerce.android.ui.woopos.roles.lock

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

enum class WooPosInactivityLockTimeout(val minutes: Int) {
    NEVER(0),
    ONE_MINUTE(1),
    TWO_MINUTES(2),
    FIVE_MINUTES(5),
    TEN_MINUTES(10),
    FIFTEEN_MINUTES(15);

    val duration: Duration? get() = if (this == NEVER) null else minutes.minutes
}
