package com.woocommerce.android.detektrules.common

import io.github.detekt.test.utils.createEnvironment
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.compileAndLintWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StringifyLambdaBearingObjectRuleTest {
    private val env = createEnvironment().env
    private val rule = StringifyLambdaBearingObjectRule(Config.empty)

    @Test
    fun `when a data class with a lambda property is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            data class Loading(val onCancel: () -> Unit)
            fun log(s: Loading): String = "state: ${'$'}s"
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `when a sealed class root whose subclass has a lambda is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            sealed class State {
                data class Loading(val onCancel: () -> Unit) : State()
                data class Done(val msg: String) : State()
            }
            fun log(s: State): String = "state: ${'$'}s"
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `when a sealed interface root whose subclass has a lambda is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            sealed interface State {
                data class Loading(val onCancel: () -> Unit) : State
                data class Done(val msg: String) : State
            }
            fun log(s: State): String = "state: ${'$'}s"
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given the 825 shape, when the when-subject is interpolated in an else branch, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            sealed interface State {
                data class UpdateRequired(val n: Int) : State
                data class UpdateAvailable(val n: Int) : State
                data class Failed(val onRetry: () -> Unit) : State
            }
            fun log(s: State): String = when (val currentState = s) {
                is State.UpdateRequired -> "a"
                is State.UpdateAvailable -> "b"
                else -> "invalid ${'$'}currentState"
            }
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given no narrowing, when the else-branch base type is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            sealed interface State {
                data class A(val n: Int) : State
                data class B(val n: Int) : State
                data class C(val n: Int) : State
                data class Failed(val onRetry: () -> Unit) : State
            }
            fun log(s: State): String = when (val cs = s) {
                is State.A -> "a"
                is State.B -> "b"
                else -> "invalid ${'$'}cs"
            }
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `given a StateFlow value on a sealed interface, when the when-subject is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            import kotlinx.coroutines.flow.MutableStateFlow
            sealed interface State {
                data class A(val n: Int) : State
                data class B(val n: Int) : State
                data class C(val n: Int) : State
                data class Failed(val onRetry: () -> Unit) : State
            }
            val flow = MutableStateFlow<State>(State.A(1))
            fun log(): String = when (val cs = flow.value) {
                is State.A -> "a"
                is State.B -> "b"
                else -> "invalid ${'$'}cs"
            }
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `when a StateFlow value of a lambda-bearing type is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            import kotlinx.coroutines.flow.MutableStateFlow
            data class Loading(val onCancel: () -> Unit)
            val flow = MutableStateFlow<Loading>(Loading({}))
            fun log(): String = "state: ${'$'}{flow.value}"
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `when a bare lambda is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            fun log(cb: () -> Unit): String = "cb: ${'$'}cb"
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `when a data class with a list-of-lambda property is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            data class Foo(val actions: List<() -> Unit>)
            fun log(s: Foo): String = "state: ${'$'}s"
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `when a data class with a map-of-lambda property is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            data class Foo(val actions: Map<String, () -> Unit>)
            fun log(s: Foo): String = "state: ${'$'}s"
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `when a bare list of lambdas is interpolated, then it is flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            fun log(actions: List<() -> Unit>): String = "actions: ${'$'}actions"
            """.trimIndent()
        )
        assertThat(findings).hasSize(1)
    }

    @Test
    fun `when a non-data class with a lambda property is interpolated, then it is not flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            class Loading(val onCancel: () -> Unit)
            fun log(s: Loading): String = "state: ${'$'}s"
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }

    @Test
    fun `when a non-collection wrapper of a lambda is interpolated, then it is not flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            fun log(c: Comparator<() -> Unit>): String = "cmp: ${'$'}c"
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }

    @Test
    fun `when a data class without a lambda property is interpolated, then it is not flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            sealed class State {
                data class A(val msg: String) : State()
                data class B(val n: Int) : State()
            }
            fun log(s: State): String = "state: ${'$'}s"
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }

    @Test
    fun `when a field of a lambda-bearing object is interpolated, then it is not flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            data class Loading(val onCancel: () -> Unit, val label: String)
            fun log(s: Loading): String = "state: ${'$'}{s.label}"
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }

    @Test
    fun `when the class name is interpolated, then it is not flagged`() {
        val findings = rule.compileAndLintWithContext(
            env,
            """
            data class Loading(val onCancel: () -> Unit)
            fun log(s: Loading): String = "state: ${'$'}{s::class.simpleName}"
            """.trimIndent()
        )
        assertThat(findings).isEmpty()
    }
}
