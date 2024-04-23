package com.woocommerce.android.app

import dagger.Binds
import dagger.Provides
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

abstract class ApplicationModule {
    @Binds
    abstract fun bindCoroutineScope(@AppCoroutineScope scope: CoroutineScope): CoroutineScope

    companion object {
        @Provides
        @AppCoroutineScope
        @Singleton
        fun provideAppCoroutineScope(dispatcher: CoroutineDispatcher): CoroutineScope =
            CoroutineScope(SupervisorJob() + dispatcher)

        @Provides
        fun provideBackgroundDispatcher(): CoroutineDispatcher {
            return Dispatchers.Default
        }
    }
}

@Qualifier
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class AppCoroutineScope
