package com.cailiangzhe.lexidue.data.remote

import com.cailiangzhe.lexidue.domain.model.DictionaryEnrichment

sealed interface DictionaryLookupResult {
    data class Success(
        val enrichment: DictionaryEnrichment,
    ) : DictionaryLookupResult

    /** The caller supplied a value outside the bounded English-word lookup contract. */
    data object InvalidRequest : DictionaryLookupResult

    /** The provider returned HTTP 404 for the requested word. */
    data object NotFound : DictionaryLookupResult

    /** A successful HTTP response could not be decoded as the documented JSON shape. */
    data object MalformedPayload : DictionaryLookupResult

    /** The request exceeded an OkHttp connect, read, or call timeout. */
    data object Timeout : DictionaryLookupResult

    /** A non-timeout transport [java.io.IOException] prevented a response. */
    data object NetworkFailure : DictionaryLookupResult

    /** A valid response contained no safely usable sense for the exact requested word. */
    data object UnusableContent : DictionaryLookupResult

    /** A non-success status other than the separately modelled 404 response. */
    data class HttpFailure(
        val statusCode: Int,
    ) : DictionaryLookupResult
}
