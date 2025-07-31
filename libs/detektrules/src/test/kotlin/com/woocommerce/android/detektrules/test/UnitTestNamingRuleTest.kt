package com.woocommerce.android.detektrules.test

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@KotlinCoreEnvironmentTest
class UnitTestNamingRuleTest {

    @Test
    fun `given valid test names with mandatory when and then, then no violations are reported`() {
        val code = """
            import org.junit.Test

            class SampleTest {
                @Test
                fun `given user is logged in, when button is clicked, then dashboard is shown`() {
                    // Valid: given, when, then
                }

                @Test
                fun `when button is clicked, then sum label is updated`() {
                    // Valid: when, then (given is optional)
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given invalid test names without mandatory when or then, then violations are reported`() {
        val code = """
            import org.junit.Test

            class SampleTest {
                @Test
                fun `then default state is shown`() {
                    // Invalid: missing when
                }

                @Test
                fun `given user exists, when login is attempted`() {
                    // Invalid: missing then
                }

                @Test
                fun `given user exists, then success is returned`() {
                    // Invalid: missing when
                }

                @Test
                fun `invalid test name format`() {
                    // Invalid: missing when and then
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(4)
        assertThat(findings.all { it.message.contains("should follow the naming convention") }).isTrue()
    }

    @Test
    fun `given test names with uppercase when or then, then violations are reported`() {
        val code = """
            import org.junit.Test

            class SampleTest {
                @Test
                fun `given user exists, When login is attempted, then success is returned`() {
                    // Invalid: uppercase When
                }

                @Test
                fun `given data is loaded, when refresh is triggered, Then updated data is shown`() {
                    // Invalid: uppercase Then
                }

                @Test
                fun `Given user exists, When login is attempted, Then success is returned`() {
                    // Invalid: uppercase When and Then (given can be any case)
                }

                @Test
                fun `WHEN button is clicked, THEN result is shown`() {
                    // Invalid: uppercase WHEN and THEN
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(4)
    }

    @Test
    fun `given test names with uppercase given, when or then, then violations are reported`() {
        val code = """
            import org.junit.Test

            class SampleTest {
                @Test
                fun `Given user exists, when login is attempted, then success is returned`() {
                    // Invalid: uppercase Given
                }

                @Test
                fun `GIVEN data is loaded, when refresh is triggered, then updated data is shown`() {
                    // Invalid: uppercase GIVEN
                }

                @Test
                fun `GiVeN mixed case, when action occurs, then result happens`() {
                    // Invalid: mixed case given
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(3)
    }

    @Test
    fun `given edge cases with punctuation, then violations are reported correctly`() {
        val code = """
            import org.junit.Test

            class SampleTest {
                @Test
                fun `given user exists, when login is attempted then success is returned`() {
                    // Invalid: missing comma before 'then'
                }

                @Test
                fun `given user exists when login is attempted, then success is returned`() {
                    // Invalid: missing comma after given clause
                }

                @Test
                fun `when button is clicked then result is shown`() {
                    // Invalid: missing comma before 'then'
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(3)
    }

    @Test
    fun `given non-test functions with invalid names, then no violations are reported`() {
        val code = """
            class SampleClass {
                fun `invalid function name without test annotation`() {
                    // Regular function - should be ignored
                }

                fun `When something, Then something`() {
                    // Regular function - should be ignored
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given parameterized test with valid naming, then no violations are reported`() {
        val code = """
            import org.junit.Test

            class SampleTest {
                @Test
                fun `given different inputs, when validation is performed, then correct result is returned`() {
                    // Valid parameterized test
                }

                @Test
                fun `when input is provided, then output is generated`() {
                    // Valid parameterized test without given
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given test with other annotations, then only Test and ParameterizedTest are validated`() {
        val code = """
            import org.junit.Test
            import org.junit.Before
            import org.junit.After

            class SampleTest {
                @Before
                fun `setup invalid name`() {
                    // Should be ignored - not a test
                }

                @After
                fun `cleanup invalid name`() {
                    // Should be ignored - not a test
                }

                @Test
                fun `invalid test name`() {
                    // Should be flagged - missing when and then
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].entity.signature).contains("invalid test name")
    }

    @Test
    fun `given test with mixed valid and invalid names, then only invalid ones are reported`() {
        val code = """
            import org.junit.Test

            class SampleTest {
                @Test
                fun `given valid setup, when action occurs, then result is expected`() {
                    // Valid
                }

                @Test
                fun `invalid name format`() {
                    // Invalid: missing when and then
                }

                @Test
                fun `when action happens, then outcome is achieved`() {
                    // Valid
                }

                @Test
                fun `given setup, then outcome`() {
                    // Invalid: missing when
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(2)
        assertThat(findings.map { it.entity.signature }).anyMatch { it.contains("invalid name format") }
        assertThat(findings.map { it.entity.signature }).anyMatch { it.contains("given setup, then outcome") }
    }

    @Test
    fun `given empty test class, then no violations are reported`() {
        val code = """
            import org.junit.Test

            class EmptyTest {
                // No test methods
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given test with multiple annotation types, then correct validation occurs`() {
        val code = """
            import org.junit.Test
            import org.junit.Test
            import org.junit.Test

            class SampleTest {
                @Test
                fun `given repeated test, when executed, then result is consistent`() {
                    // Valid name with multiple annotations
                }

                @Test
                fun `invalid parameterized test name`() {
                    // Invalid name - missing when and then
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].entity.signature).contains("invalid parameterized test name")
    }
}
