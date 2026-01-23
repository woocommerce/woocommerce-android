package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
internal class TimestampMediaIdGeneratorTest {

    lateinit var sut: TimestampMediaIdGenerator

    var currentMillis = 123L
    private val fakeClock = object : Clock {
        override fun now(): Instant {
            return Instant.fromEpochMilliseconds(currentMillis)
        }
    }

    @Before
    fun setUp() {
        sut = TimestampMediaIdGenerator(fakeClock)
    }

    @Test
    fun `when generating with same inputs, then it returns consistent ID`() {
        val id1 = sut.generate("/path/to/file.jpg")
        val id2 = sut.generate("/path/to/file.jpg")

        Assertions.assertThat(id1).isEqualTo(id2)
    }

    @Test
    fun `when generating with different file paths, then it returns different IDs`() {
        val id1 = sut.generate("/path/to/file1.jpg")
        val id2 = sut.generate("/path/to/file2.jpg")

        Assertions.assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `when generating with different timestamps, then it returns different IDs`() {
        currentMillis = 1000L
        val id1 = sut.generate("/path/to/file.jpg")
        currentMillis = 2000L
        val id2 = sut.generate("/path/to/file.jpg")

        Assertions.assertThat(id1).isNotEqualTo(id2)
    }
}
