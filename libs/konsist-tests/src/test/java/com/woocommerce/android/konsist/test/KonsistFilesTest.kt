package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

class KonsistFilesTest {
    @Test
    fun `no empty files allowed`() {
        Konsist.scopeFromProject()
            .files
            .assertFalse { it.text.isEmpty() }
    }
}
