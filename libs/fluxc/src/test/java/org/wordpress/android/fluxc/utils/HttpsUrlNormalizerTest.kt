package org.wordpress.android.fluxc.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.Test

class HttpsUrlNormalizerTest {
    private val normalizer = HttpsUrlNormalizer()

    @Test
    fun `given HTTP URL, when normalizing, then upgrade only its transport`() {
        val result = normalizer.normalize("http://example.com/store%2Fpath?q=a%2Fb#section")

        assertThat(result.normalizedUrl).isEqualTo("https://example.com/store%2Fpath?q=a%2Fb#section")
        assertThat(result.wasUpgraded).isTrue()
    }

    @Test
    fun `given uppercase HTTP scheme, when normalizing, then upgrade to HTTPS`() {
        val result = normalizer.normalize("HTTP://example.com/path")

        assertThat(result.normalizedUrl).isEqualTo("https://example.com/path")
        assertThat(result.wasUpgraded).isTrue()
    }

    @Test
    fun `given HTTPS URL, when normalizing, then keep it secure`() {
        val result = normalizer.normalize("https://example.com/path")

        assertThat(result.normalizedUrl).isEqualTo("https://example.com/path")
        assertThat(result.wasUpgraded).isFalse()
    }

    @Test
    fun `given URL has query and fragment without path, when normalizing, then avoid adding a path`() {
        val result = normalizer.normalize("http://example.com?q=a%2Fb#section")

        assertThat(result.normalizedUrl).isEqualTo("https://example.com?q=a%2Fb#section")
    }

    @Test
    fun `given scheme-less login input, when adding a scheme, then default to HTTPS`() {
        val result = normalizer.normalize("example.com/store", addHttpsSchemeIfMissing = true)

        assertThat(result.normalizedUrl).isEqualTo("https://example.com/store")
        assertThat(result.wasUpgraded).isFalse()
    }

    @Test
    fun `given HTTP default port, when normalizing, then use HTTPS default port`() {
        val result = normalizer.normalize("http://example.com:80/path")

        assertThat(result.normalizedUrl).isEqualTo("https://example.com/path")
    }

    @Test
    fun `given HTTP custom port, when normalizing, then preserve the port`() {
        val result = normalizer.normalize("http://example.com:8080/path")

        assertThat(result.normalizedUrl).isEqualTo("https://example.com:8080/path")
    }

    @Test
    fun `given unsupported or malformed URL, when normalizing, then reject it`() {
        listOf("ftp://example.com", "not a url", "https://").forEach { url ->
            assertThatIllegalArgumentException().isThrownBy { normalizer.normalize(url) }
        }
    }
}
