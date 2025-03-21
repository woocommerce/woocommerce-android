package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.functions
import com.lemonappdev.konsist.api.ext.list.returnTypes
import com.lemonappdev.konsist.api.ext.list.withoutSourceSet
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class KonsistFunctionsTest {
    @Test // Or suppress all files: @file:Suppress("konsist.return type of all functions are immutable")
    fun `return type of all functions - expect in a test - are immutable`() {
        Konsist.scopeFromProject()
            .files
            .filter { file ->
                !file.path.contains("FlowExt.kt") &&
                    !file.path.contains("LiveDataExt.kt") &&
                    !file.path.contains("SavedStateFlow.kt")
            }
            .functions()
            .returnTypes
            .withoutSourceSet("test")
            .assertFalse { it.isMutableType }
    }
}
