package com.woocommerce.android.di

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.util.helpers.Debouncer
import javax.inject.Named

const val UI_THREAD = "UI_THREAD"
const val BG_THREAD = "BG_THREAD"
const val IO_THREAD = "IO_THREAD"

@Module
class ThreadModule {
    @Provides
    @Named(UI_THREAD)
    fun provideUiDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main
    }

    @Provides
    @Named(BG_THREAD)
    fun provideBackgroundDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @Provides
    @Named(IO_THREAD)
    fun provideIoDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }

    @Provides
    fun provideDebouncer(): Debouncer {
        return Debouncer()
    }
}
