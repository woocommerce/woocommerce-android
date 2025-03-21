package com.woocommerce.android.konsist.test

import androidx.compose.ui.tooling.preview.Preview
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAnnotationOf
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

class KonsistComposeTest {
    @Test
    fun `all jetpack compose previews contain 'preview' in method name`() {
        Konsist.scopeFromProject()
            .functions()
            .withAnnotationOf(Preview::class)
            .assertTrue { it.hasNameContaining("Preview") }
    }
}
