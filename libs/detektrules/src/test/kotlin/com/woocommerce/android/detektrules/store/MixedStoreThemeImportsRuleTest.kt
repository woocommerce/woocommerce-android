package com.woocommerce.android.detektrules.store

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@KotlinCoreEnvironmentTest
class MixedStoreThemeImportsRuleTest {

    @Test
    fun `given a Store file imports only the legacy theme, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import com.woocommerce.android.ui.compose.theme.WooTheme
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given a Store file imports only the design system, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import com.woocommerce.android.ui.compose.designsystem.component.WooButton
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given a Store file imports both theme roots, when linting, then one actionable violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import com.woocommerce.android.ui.compose.designsystem.component.WooButton
            import com.woocommerce.android.ui.compose.theme.WooTheme
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single().message)
            .contains("Use one Store theme root per file")
            .contains("migrate the remaining imports")
    }

    @Test
    fun `given mixed theme imports use aliases, when linting, then one violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import com.woocommerce.android.ui.compose.designsystem.foundation.WooTheme as StoreTheme
            import com.woocommerce.android.ui.compose.theme.WooTheme as LegacyTheme
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given multiple imports from both theme roots, when linting, then only one violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import com.woocommerce.android.ui.compose.designsystem.component.WooButton
            import com.woocommerce.android.ui.compose.designsystem.foundation.WooTheme
            import com.woocommerce.android.ui.compose.theme.WooColors
            import com.woocommerce.android.ui.compose.theme.WooTheme
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given a lookalike legacy theme package, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import com.woocommerce.android.ui.compose.designsystem.component.WooButton
            import com.woocommerce.android.ui.compose.themes.WooTheme
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given a lookalike design system package, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import com.woocommerce.android.ui.compose.designsystems.component.WooButton
            import com.woocommerce.android.ui.compose.theme.WooTheme
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given a WooPos file imports both Store theme roots, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.woopos.orders

            import com.woocommerce.android.ui.compose.designsystem.component.WooButton
            import com.woocommerce.android.ui.compose.theme.WooTheme
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given a similarly named Store package imports both roots, when linting, then one violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.wooposition

            import com.woocommerce.android.ui.compose.designsystem.component.WooButton
            import com.woocommerce.android.ui.compose.theme.WooTheme
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given a non-Store file imports both theme roots, when linting, then no violation is reported`() {
        val code = """
            package com.example.feature

            import com.woocommerce.android.ui.compose.designsystem.component.WooButton
            import com.woocommerce.android.ui.compose.theme.WooTheme
        """.trimIndent()

        val findings = MixedStoreThemeImportsRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }
}
