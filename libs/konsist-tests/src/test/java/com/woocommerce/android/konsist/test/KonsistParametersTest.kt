package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.modifierprovider.withValueModifier
import com.lemonappdev.konsist.api.ext.list.primaryConstructors
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class KonsistParametersTest {
    @Test
    fun `every value class has parameter named 'value'`() {
        Konsist.scopeFromProject()
            .classes()
            .withValueModifier()
            .primaryConstructors
            .assertTrue { it.hasParameterWithName("value") }
    }
}
