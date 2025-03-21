package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.properties
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class KonsistFieldsTest {
    @Test
    fun `no field should have 'm' prefix`() {
        Konsist.scopeFromProject()
            .classes()
            .properties()
            .assertFalse {
                val secondCharacterIsUppercase = it.name.getOrNull(1)?.isUpperCase() ?: false
                it.name.startsWith('m') && secondCharacterIsUppercase
            }
    }
}
