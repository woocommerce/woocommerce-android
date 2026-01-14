package org.wordpress.android.fluxc.module

import dagger.Binds
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.store.MediaIdGenerator
import org.wordpress.android.fluxc.store.TimestampMediaIdGenerator
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Module
interface MediaModule {
    @Binds
    fun bindMediaIdGenerator(generator: TimestampMediaIdGenerator): MediaIdGenerator

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.System
    }
}
