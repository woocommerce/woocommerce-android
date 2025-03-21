package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class KonsistImportsTest {
    @Test // Detekt: WildcardImport (https://detekt.dev/docs/rules/style/#wildcardimport)
    fun `no wildcard imports allowed`() {
        Konsist.scopeFromProject()
            .imports
            .assertFalse { it.isWildcard }
    }
}
