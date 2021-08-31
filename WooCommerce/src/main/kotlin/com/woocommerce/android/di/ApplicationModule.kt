package com.woocommerce.android.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjectionModule
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import org.wordpress.android.login.di.LoginServiceModule
import javax.inject.Qualifier
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

    companion object {
        @Provides
        @AppCoroutineScope
        fun provideAppCoroutineScope(): CoroutineScope = GlobalScope
    }
}

@Qualifier
@MustBeDocumented
@Retention(RUNTIME)
annotation class AppCoroutineScope
