package com.woocommerce.android.di

import com.woocommerce.android.util.CoroutineDispatchers
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.util.helpers.Debouncer

@InstallIn(SingletonComponent::class)
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
