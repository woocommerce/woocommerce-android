package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@KotlinCoreEnvironmentTest
class WooPosDesignSystemSpacingUsageRuleTest {

    @Test
    fun `given raw dp in padding modifier, when linting, then violation is reported`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.foundation.layout.padding
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            fun usage() {
                Modifier.padding(16.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemSpacingUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given raw dp in PaddingValues positional arg, when linting, then violation is reported`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.foundation.layout.PaddingValues
            import androidx.compose.ui.unit.dp

            fun usage() {
                PaddingValues(16.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemSpacingUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given raw dp in PaddingValues named args, when linting, then violations are reported`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.foundation.layout.PaddingValues
            import androidx.compose.ui.unit.dp

            fun usage() {
                PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemSpacingUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).hasSize(2)
    }

    @Test
    fun `given raw dp in Arrangement spacedBy, when linting, then violation is reported`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.foundation.layout.Arrangement
            import androidx.compose.ui.unit.dp

            fun usage() {
                Arrangement.spacedBy(8.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemSpacingUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given raw dp in Arrangement Absolute spacedBy, when linting, then violation is reported`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.foundation.layout.Arrangement
            import androidx.compose.ui.unit.dp

            fun usage() {
                Arrangement.Absolute.spacedBy(8.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemSpacingUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given PaddingValues uses WooPosSpacing, when linting, then no violation`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.foundation.layout.PaddingValues

            object WooPosSpacing { object Medium { val value = 16 } }

            fun usage() {
                PaddingValues(WooPosSpacing.Medium.value)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemSpacingUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).isEmpty()
    }

    @Test
    fun `given spacedBy uses WooPosSpacing, when linting, then no violation`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.foundation.layout.Arrangement

            object WooPosSpacing { object Small { val value = 8 } }

            fun usage() {
                Arrangement.spacedBy(WooPosSpacing.Small.value)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemSpacingUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).isEmpty()
    }

    @Test
    fun `given raw dp outside woopos package, when linting, then no violations`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.other

            import androidx.compose.foundation.layout.Arrangement
            import androidx.compose.foundation.layout.PaddingValues
            import androidx.compose.foundation.layout.padding
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            fun usage() {
                Modifier.padding(16.dp)
                PaddingValues(16.dp)
                Arrangement.spacedBy(8.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemSpacingUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).isEmpty()
    }
}
