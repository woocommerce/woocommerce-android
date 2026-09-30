package org.wordpress.android.fluxc.network

data class UnexpectedStoreResponse(
    val kind: UnexpectedStoreResponseKind,
    val statusCode: Int,
    val contentType: String?,
    val requestType: String,
    val excerpt: String?
) {
    companion object {
        fun from(statusCode: Int, contentType: String?, body: String, requestType: String): UnexpectedStoreResponse? {
            val kind = UnexpectedStoreResponseClassifier.classify(statusCode, contentType, body) ?: return null
            return UnexpectedStoreResponse(
                kind = kind,
                statusCode = statusCode,
                contentType = contentType,
                requestType = requestType,
                excerpt = UnexpectedStoreResponseExcerpt.from(body)
            )
        }
    }
}
