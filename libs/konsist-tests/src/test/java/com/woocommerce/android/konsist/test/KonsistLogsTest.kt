package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withoutSourceSet
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class KonsistLogsTest {
    @Test
    fun `no class should use java util logging`() {
        Konsist.scopeFromProject()
            .files
            .assertFalse { it.hasImport { import -> import.name == "java.util.logging.." } }
    }

    @Test // Or suppress 'WooLog' file: @file:Suppress("konsist.return type of all functions are immutable")
    fun `no class should use android util logging`() {
        Konsist.scopeFromProject()
            .files
            .filter { file ->
                !file.path.contains("WooLog.kt")
            }
            .withoutSourceSet("androidTest")
            .assertFalse { it.hasImport { import -> import.name == "android.util.Log" } }
    }
}
