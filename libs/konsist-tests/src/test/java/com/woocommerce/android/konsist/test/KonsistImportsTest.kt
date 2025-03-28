package com.woocommerce.android.konsist.test

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.imports
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.Test

@Suppress("MaxLineLength")
class KonsistImportsTest {
    @Test // Detekt: WildcardImport (https://detekt.dev/docs/rules/style/#wildcardimport)
    fun `no wildcard imports allowed`() {
        Konsist.scopeFromProject()
            .imports
            .assertFalse { it.isWildcard }
    }

    @Test // Custom Detekt: WooPosDesignSystemButtonUsageRule (https://github.com/woocommerce/woocommerce-android/blob/trunk/libs/detektrules/src/main/kotlin/com/woocommerce/android/detektrules/woopos/WooPosDesignSystemButtonUsageRule.kt#L13)
    fun `woopos package - no standard compose buttons imports allowed - use woopos button instead`() {
        Konsist.scopeFromPackage("com.woocommerce.android.ui.woopos..")
            .files
            .filter { file ->
                !file.path.contains("WooPosButtons.kt")
            }
            .imports
            .assertFalse { it.hasNameMatching("""^androidx\.compose\.material3\.Button${'$'}""".toRegex()) }
    }

    @Test // Custom Detekt: WooPosDesignSystemTextUsageRule (https://github.com/woocommerce/woocommerce-android/blob/trunk/libs/detektrules/src/main/kotlin/com/woocommerce/android/detektrules/woopos/WooPosDesignSystemTextUsageRule.kt#L13)
    fun `woopos package - no standard compose text imports allowed - use woopos text instead`() {
        Konsist.scopeFromPackage("com.woocommerce.android.ui.woopos..")
            .files
            .filter { file ->
                !file.path.contains("WooPosTexts.kt")
            }
            .imports
            .assertFalse { it.hasNameMatching("""^androidx\.compose\.material3\.Text${'$'}""".toRegex()) }
    }
}
