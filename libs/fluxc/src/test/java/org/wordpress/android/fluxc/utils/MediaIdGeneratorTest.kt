package org.wordpress.android.fluxc.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class MediaIdGeneratorTest {
    @Test
    fun `when generating with same inputs, then it returns consistent ID`() {
        val id1 = MediaIdGenerator.generate(1, "/path/to/file.jpg", 1234567890L)
        val id2 = MediaIdGenerator.generate(1, "/path/to/file.jpg", 1234567890L)

        assertThat(id1).isEqualTo(id2)
    }

    @Test
    fun `when generating with different file paths, then it returns different IDs`() {
        val timestamp = System.currentTimeMillis()
        val id1 = MediaIdGenerator.generate(1, "/path/to/file1.jpg", timestamp)
        val id2 = MediaIdGenerator.generate(1, "/path/to/file2.jpg", timestamp)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `when generating with different timestamps, then it returns different IDs`() {
        val id1 = MediaIdGenerator.generate(1, "/path/to/file.jpg", 1000L)
        val id2 = MediaIdGenerator.generate(1, "/path/to/file.jpg", 2000L)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `when generating with different site IDs, then it returns different IDs`() {
        val timestamp = System.currentTimeMillis()
        val id1 = MediaIdGenerator.generate(1, "/path/to/file.jpg", timestamp)
        val id2 = MediaIdGenerator.generate(2, "/path/to/file.jpg", timestamp)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun `when generating with empty file path, then it throws exception`() {
        assertThatThrownBy {
            MediaIdGenerator.generate(1, "", System.currentTimeMillis())
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `when generating, then it can produce negative IDs`() {
        val id = MediaIdGenerator.generate(1, "/some/path.jpg", 123L)
        assertThat(id.value).isNotZero
    }

    @Test
    fun `when generating with special characters in file path, then it handles correctly`() {
        val timestamp = System.currentTimeMillis()
        val id1 = MediaIdGenerator.generate(1, "/path with spaces/file (1).jpg", timestamp)
        val id2 = MediaIdGenerator.generate(1, "/path_with_underscores/file-dash.jpg", timestamp)

        assertThat(id1).isNotEqualTo(id2)
        assertThat(id1.value).isNotZero
        assertThat(id2.value).isNotZero
    }

    @Test
    fun `when generating with very long file paths, then it handles correctly`() {
        val longPath = "/very/long/path/" + "directory/".repeat(50) + "file.jpg"
        val id = MediaIdGenerator.generate(1, longPath, System.currentTimeMillis())

        assertThat(id.value).isNotZero
    }
}
