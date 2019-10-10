package com.woocommerce.android.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Application context aware {@link ScopedViewModel}
 * <p>
 * Subclasses must have a constructor which accepts {@link Application} as the only parameter.
 * <p>
 */
@SuppressLint("StaticFieldLeak")
abstract class ScopedAndroidViewModel(
    defaultDispatcher: CoroutineDispatcher,
    val appContext: Application
) : ScopedViewModel(defaultDispatcher)
