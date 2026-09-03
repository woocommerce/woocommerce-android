package com.woocommerce.android.detektrules.store

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@KotlinCoreEnvironmentTest
class StoreTopAppBarIconButtonUsageRuleTest {
    @Test
    fun `given Material3 IconButton in WooTopAppBar actions, when linting, then violation points to IconAction`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title", actions = {
                    IconButton(onClick = {}) {}
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings.single().message).contains("Use IconAction")
    }

    @Test
    fun `given Material IconButton in trailing WooTopAppBar actions, when linting, then violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title") {
                    IconButton(onClick = {}) {}
                }
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given aliased WooTopAppBar and IconButton imports, when linting, then violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton as MaterialIconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar as StoreTopAppBar

            fun usage() {
                StoreTopAppBar(title = "Title", actions = {
                    MaterialIconButton(onClick = {}) {}
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given fully qualified IconButton in nested actions content, when linting, then violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title", actions = {
                    if (true) {
                        androidx.compose.material3.IconButton(onClick = {}) {}
                    }
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given IconButton in actions receiver helper, when linting, then violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBarActionsScope

            fun WooTopAppBarActionsScope.Actions() {
                IconButton(onClick = {}) {}
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given IconButton in DropdownMenu content, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.DropdownMenu
            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title", actions = {
                    DropdownMenu(expanded = false, onDismissRequest = {}) {
                        IconButton(onClick = {}) {}
                    }
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given a fully qualified top app bar owner, when linting, then violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton

            fun usage() {
                com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar(
                    title = "Title",
                    actions = {
                        IconButton(onClick = {}) {}
                    },
                )
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given positional actions lambda, when linting, then violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(
                    "Title",
                    Modifier,
                    Icons.Default.ArrowBack,
                    "Back",
                    {},
                    WindowInsets(0),
                    { IconButton(onClick = {}) {} },
                )
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given a trailing lambda past the positional actions index, when linting, then violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(
                    { Text("Title") },
                    Modifier,
                    {},
                    WindowInsets(0),
                    { IconButton(onClick = {}) {} },
                )
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given a trailing lambda at the positional actions index, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(
                    { Text("Title") },
                    Modifier,
                    {},
                    { IconButton(onClick = {}) {} },
                )
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given wildcard top app bar import, when linting, then violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.*

            fun usage() {
                WooTopAppBar(title = "Title", actions = {
                    IconButton(onClick = {}) {}
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given actions lambda in a property, when linting, then violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBarActionsScope

            val actions: WooTopAppBarActionsScope.() -> Unit = {
                IconButton(onClick = {}) {}
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given IconButton in WooOverflowMenu content, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooOverflowMenu
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title", actions = {
                    WooOverflowMenu(trigger = { onClick -> onClick() }) {
                        IconButton(onClick = {}) {}
                    }
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given IconButton in OverflowAction content, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title", actions = {
                    OverflowAction(contentDescription = "More") { dismiss ->
                        IconButton(onClick = dismiss) {}
                    }
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given IconButton in a positional title lambda, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(
                    { IconButton(onClick = {}) {} },
                    Modifier,
                )
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given a composable title lambda as the only argument, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar({ IconButton(onClick = {}) {} })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given a qualified non target owner, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                Other.WooTopAppBar(title = "Title", actions = {
                    IconButton(onClick = {}) {}
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given a qualified non material IconButton, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title", actions = {
                    Other.IconButton(onClick = {}) {}
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given IconButton outside top app bar actions, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title")
                IconButton(onClick = {}) {}
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given an unrelated top app bar of the same name, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.IconButton
            import com.example.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title", actions = {
                    IconButton(onClick = {}) {}
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given inline custom content and scoped actions, when linting, then no violation is reported`() {
        val code = """
            package com.woocommerce.android.ui.orders

            import androidx.compose.material3.CircularProgressIndicator
            import androidx.compose.material3.DropdownMenu
            import androidx.compose.material3.Text
            import com.woocommerce.android.ui.compose.designsystem.component.WooTopAppBar

            fun usage() {
                WooTopAppBar(title = "Title", actions = {
                    Text("2/4")
                    CircularProgressIndicator()
                    DropdownMenu(expanded = false, onDismissRequest = {}) {}
                    IconAction(imageVector = Any(), contentDescription = "Share", onClick = {})
                })
            }
        """.trimIndent()

        val findings = StoreTopAppBarIconButtonUsageRule(Config.empty).compileAndLint(code)

        assertThat(findings).isEmpty()
    }
}
