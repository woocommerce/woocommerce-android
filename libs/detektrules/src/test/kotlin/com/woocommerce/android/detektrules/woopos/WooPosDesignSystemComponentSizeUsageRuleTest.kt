package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@KotlinCoreEnvironmentTest
class WooPosDesignSystemComponentSizeUsageRuleTest {

    @Test
    fun `given raw dp in size modifiers, when linting, then violations are reported`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.foundation.layout.defaultMinSize
            import androidx.compose.foundation.layout.height
            import androidx.compose.foundation.layout.heightIn
            import androidx.compose.foundation.layout.size
            import androidx.compose.foundation.layout.width
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            fun usage() {
                Modifier.size(24.dp)
                Modifier.height(80.dp)
                Modifier.width(604.dp)
                Modifier.heightIn(min = 64.dp)
                Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemComponentSizeUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).hasSize(6)
    }

    @Test
    fun `given adaptive component size or design system size, when linting, then no violations`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.foundation.layout.size
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            fun Int.toAdaptiveComponentSize() = this
            fun Int.toAdaptiveIconSize() = this

            fun usage() {
                Modifier.size(24.dp.toAdaptiveComponentSize())
                Modifier.size(24.dp.toAdaptiveIconSize())
                Modifier.size(WooPosIconSize.Medium.value)
            }

            object WooPosIconSize { object Medium { val value = 24 } }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemComponentSizeUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).isEmpty()
    }

    @Test
    fun `given raw dp outside woopos package, when linting, then no violations`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.other

            import androidx.compose.foundation.layout.size
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            fun usage() {
                Modifier.size(24.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemComponentSizeUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).isEmpty()
    }
}
