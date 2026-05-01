package com.woocommerce.android.detektrules.woopos

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@KotlinCoreEnvironmentTest
class WooPosDesignSystemElevationUsageRuleTest {

    @Test
    fun `given raw dp in shadow and elevation calls, when linting, then violations are reported`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.material3.CardDefaults
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.draw.shadow
            import androidx.compose.ui.unit.dp

            fun usage() {
                Modifier.shadow(8.dp)
                Modifier.shadow(elevation = 8.dp)
                CardDefaults.cardElevation(defaultElevation = 4.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemElevationUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).hasSize(3)
    }

    @Test
    fun `given WooPosElevation is used, when linting, then no violations`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.woopos.sample

            import androidx.compose.ui.Modifier
            import androidx.compose.ui.draw.shadow

            object WooPosElevation { object Medium { val value = 0 } }

            fun usage() {
                Modifier.shadow(WooPosElevation.Medium.value)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemElevationUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).isEmpty()
    }

    @Test
    fun `given raw dp outside woopos package, when linting, then no violations`() {
        // GIVEN
        val code = """
            package com.woocommerce.android.ui.other

            import androidx.compose.ui.Modifier
            import androidx.compose.ui.draw.shadow
            import androidx.compose.ui.unit.dp

            fun usage() {
                Modifier.shadow(8.dp)
            }
        """.trimIndent()

        // WHEN
        val findings = WooPosDesignSystemElevationUsageRule(Config.empty).compileAndLint(code)

        // THEN
        assertThat(findings).isEmpty()
    }
}
