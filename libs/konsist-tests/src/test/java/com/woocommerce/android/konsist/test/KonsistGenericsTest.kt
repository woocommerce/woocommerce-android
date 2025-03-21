package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.declaration.flatten
import com.lemonappdev.konsist.api.ext.list.types
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class KonsistGenericsTest {
    @Test
    fun `property generic type does not contains star projection`() {
        Konsist.scopeFromProduction()
            .properties()
            .types
            .assertFalse { type ->
                type.typeArguments
                    ?.flatten()
                    ?.any { it.isStarProjection }
            }
    }
}
