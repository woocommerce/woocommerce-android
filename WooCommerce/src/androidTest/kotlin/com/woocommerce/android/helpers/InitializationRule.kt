package com.woocommerce.android.helpers

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.woocommerce.android.AppInitializer
import com.woocommerce.android.di.AppCoroutineScope
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class InitializationRule : TestRule {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppEntryPoint {
        fun initializer(): AppInitializer
        @AppCoroutineScope fun appCoroutineScope(): CoroutineScope
    }

    private val instrumentation
        get() = InstrumentationRegistry.getInstrumentation()

    override fun apply(base: Statement?, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                val application = instrumentation.targetContext.applicationContext as Application
                val entryPoint = EntryPoints.get(
                    application,
                    AppEntryPoint::class.java
                )
                instrumentation.runOnMainSync {
                    entryPoint.initializer().init(application)
                }
                base?.evaluate()
                entryPoint.appCoroutineScope().cancel()
            }
        }
    }
}
