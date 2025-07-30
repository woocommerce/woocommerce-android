package com.woocommerce.android.detektrules.test

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@KotlinCoreEnvironmentTest
class UnitTestNamingRuleTest {

    @Test
    fun `given valid test names with given when then format, then no violations are reported`() {
        val code = """
            import org.junit.jupiter.api.Test
            
            class SampleTest {
                @Test
                fun `given user is logged in, when button is clicked, then dashboard is shown`() {
                    // Test implementation
                }
                
                @Test
                fun `when button is clicked, then sum label is updated`() {
                    // Test implementation
                }
                
                @Test
                fun `then default state is shown`() {
                    // Test implementation
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given invalid test names without proper format, then violations are reported`() {
        val code = """
            import org.junit.jupiter.api.Test
            
            class SampleTest {
                @Test
                fun `invalid test name format`() {
                    // Test implementation
                }
                
                @Test
                fun testSomething() {
                    // Test implementation
                }
                
                @Test
                fun `just a description without proper format`() {
                    // Test implementation
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(3)
        assertThat(findings[0].message).contains("should follow the naming convention")
        assertThat(findings[1].message).contains("should follow the naming convention")
        assertThat(findings[2].message).contains("should follow the naming convention")
    }

    @Test
    fun `given non-test functions with invalid names, then no violations are reported`() {
        val code = """
            class SampleClass {
                fun `invalid function name without test annotation`() {
                    // Regular function
                }
                
                fun regularFunction() {
                    // Regular function
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
            import org.junit.jupiter.params.ParameterizedTest
            
            class SampleTest {
                @ParameterizedTest
                fun `given different inputs, when validation is performed, then correct result is returned`() {
                    // Test implementation
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given case insensitive naming variations, then no violations are reported`() {
        val code = """
            import org.junit.jupiter.api.Test
            
            class SampleTest {
                @Test
                fun `Given user exists, When login is attempted, Then success is returned`() {
                    // Test implementation
                }
                
                @Test
                fun `GIVEN data is loaded, WHEN refresh is triggered, THEN updated data is shown`() {
                    // Test implementation
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).isEmpty()
    }

    @Test
    fun `given edge cases with punctuation and special characters, then violations are reported correctly`() {
        val code = """
            import org.junit.jupiter.api.Test
            
            class SampleTest {
                @Test
                fun `given user exists, when login is attempted then success is returned`() {
                    // Missing comma before 'then'
                }
                
                @Test
                fun `given user exists when login is attempted, then success is returned`() {
                    // Missing comma after 'given'
                }
                
                @Test
                fun `when button is clicked then result is shown`() {
                    // Missing comma before 'then'
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(3)
    }

    @Test
    fun `given test with other annotations, then only Test and ParameterizedTest are validated`() {
        val code = """
            import org.junit.jupiter.api.Test
            import org.junit.jupiter.api.BeforeEach
            import org.junit.jupiter.api.AfterEach
            
            class SampleTest {
                @BeforeEach
                fun `setup invalid name`() {
                    // Should be ignored
                }
                
                @AfterEach
                fun `cleanup invalid name`() {
                    // Should be ignored
                }
                
                @Test
                fun `invalid test name`() {
                    // Should be flagged
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
            import org.junit.jupiter.api.Test
            
            class SampleTest {
                @Test
                fun `given valid setup, when action occurs, then result is expected`() {
                    // Valid
                }
                
                @Test
                fun `invalid name format`() {
                    // Invalid
                }
                
                @Test
                fun `when action happens, then outcome is achieved`() {
                    // Valid
                }
                
                @Test
                fun anotherInvalidName() {
                    // Invalid
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(2)
        assertThat(findings.map { it.entity.signature }).containsExactlyInAnyOrder(
            "invalid name format",
            "anotherInvalidName"
        )
    }

    @Test
    fun `given empty test class, then no violations are reported`() {
        val code = """
            import org.junit.jupiter.api.Test
            
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
            import org.junit.jupiter.api.Test
            import org.junit.jupiter.params.ParameterizedTest
            import org.junit.jupiter.api.RepeatedTest
            
            class SampleTest {
                @Test
                @RepeatedTest(5)
                fun `given repeated test, when executed, then result is consistent`() {
                    // Valid name with multiple annotations
                }
                
                @ParameterizedTest
                fun `invalid parameterized test name`() {
                    // Invalid name
                }
            }
        """.trimIndent()

        val rule = UnitTestNamingRule(Config.empty)
        val findings = rule.compileAndLint(code)

        assertThat(findings).hasSize(1)
        assertThat(findings[0].entity.signature).contains("invalid parameterized test name")
    }
}
