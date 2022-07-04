package com.woocommerce.android.di

import android.content.Context
import com.woocommerce.android.tracker.DataStoreTrackerRepository
import com.woocommerce.android.tracker.TrackerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjectionModule
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.wordpress.android.login.di.LoginServiceModule
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.annotation.AnnotationRetention.RUNTIME

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
        AndroidInjectionModule::class,
        LoginServiceModule::class
    ]
)
abstract class ApplicationModule {
    // Expose Application as an injectable context
    @Binds
    internal abstract fun bindContext(@ApplicationContext context: Context): Context

    @Binds
    abstract fun bindCoroutineScope(@AppCoroutineScope scope: CoroutineScope): CoroutineScope

    @Binds
    abstract fun bindTrackerRepository(repository: DataStoreTrackerRepository): TrackerRepository

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
@Retention(RUNTIME)
annotation class AppCoroutineScope
