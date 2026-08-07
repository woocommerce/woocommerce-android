package com.woocommerce.android.ui.login.auto

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

internal fun interface AutoLoginRequestHandler {
    suspend fun login(request: AutoLoginRequest): AutoLoginResult
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AutoLoginRequestHandlerModule {
    @Binds
    abstract fun bindAutoLoginRequestHandler(handler: AutoLoginHandler): AutoLoginRequestHandler
}
