package com.woocommerce.android.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.android.AndroidInjectionModule
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.login.di.LoginServiceModule

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
}
