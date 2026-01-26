package com.woocommerce.android.di

import dagger.BindsInstance
import dagger.hilt.DefineComponent
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Qualifier
import javax.inject.Scope
import kotlin.annotation.AnnotationRetention.RUNTIME

@Scope
@MustBeDocumented
@Retention(value = RUNTIME)
annotation class SiteScope

@SiteScope
@DefineComponent(parent = SingletonComponent::class)
interface SiteComponent {
    @DefineComponent.Builder
    interface Builder {
        fun setSite(@BindsInstance site: SiteModel): Builder
        fun setCoroutineScope(@BindsInstance @SiteCoroutineScope scope: CoroutineScope): Builder
        fun build(): SiteComponent
    }
}

@Qualifier
@MustBeDocumented
@Retention(RUNTIME)
annotation class SiteCoroutineScope
