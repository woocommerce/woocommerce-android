package com.woocommerce.android.konsist.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withAnnotationOf
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

@Suppress("MaxLineLength")
class KonsistComposeTest {
    @Test
    fun `all jetpack compose previews contain 'preview' in method name`() {
        Konsist.scopeFromProject()
            .functions()
            .withAnnotationOf(Preview::class)
            .assertTrue { it.hasNameContaining("Preview") }
    }

    /**
     * [Composable Functions Best Practices ✅](https://github.com/woocommerce/woocommerce-android/blob/trunk/docs/compose.md#composable-functions-best-practices--)
     *
     * Don't acquire the viewModel inside a composable function, this will make testing harder. Inject it as a parameter
     * and provide a default value to facilitate reusability:
     *
     * ❌
     *
     * ```
     * @Composable
     * fun MyComposable() {
     * 	val viewModel by viewModel<MyViewModel>()
     * 	...
     * }
     * ```
     *
     * ✅
     *
     * ```
     * @Composable
     * fun MyComposable(viewModel : MyViewModel = getViewModel()) {
     * 	...
     * }
     * ```
     */
    @Test
    fun `composable functions should not acquire view model directly`() {
        Konsist.scopeFromProject()
            .functions()
            .withAnnotationOf(Composable::class)
            .assertTrue { function ->
                !function.text.contains("""(?i)val\s+\w*viewModel""".toRegex())
            }
    }
}
