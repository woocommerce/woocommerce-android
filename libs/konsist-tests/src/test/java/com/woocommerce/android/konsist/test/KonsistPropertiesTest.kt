package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import com.lemonappdev.konsist.api.ext.list.indexOfFirstInstance
import com.lemonappdev.konsist.api.ext.list.indexOfLastInstance
import com.lemonappdev.konsist.api.ext.list.withoutSourceSet
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class KonsistPropertiesTest {
    @Test
    fun `properties - expect in a test - are declared before functions`() {
        Konsist.scopeFromProject()
            .classes()
            .withoutSourceSet("test")
            .assertTrue {
                val lastKoPropertyDeclarationIndex = it
                    .declarations(
                        includeNested = false,
                        includeLocal = false,
                    )
                    .indexOfLastInstance<KoPropertyDeclaration>()
                val firstKoFunctionDeclarationIndex = it
                    .declarations(
                        includeNested = false,
                        includeLocal = false,
                    )
                    .indexOfFirstInstance<KoFunctionDeclaration>()
                if (lastKoPropertyDeclarationIndex != -1 && firstKoFunctionDeclarationIndex != -1) {
                    lastKoPropertyDeclarationIndex < firstKoFunctionDeclarationIndex
                } else {
                    true
                }
            }
    }
}
