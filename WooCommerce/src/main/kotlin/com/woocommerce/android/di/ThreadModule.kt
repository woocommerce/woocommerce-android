package com.woocommerce.android.di

import com.woocommerce.android.util.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import org.wordpress.android.util.helpers.Debouncer

@Module
class ThreadModule {
    @Provides
    fun provideDispatchers(): CoroutineDispatchers {
        return CoroutineDispatchers()
    }

    @Provides
    fun provideDebouncer(): Debouncer {
        return Debouncer()
    }
}
