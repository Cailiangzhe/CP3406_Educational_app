package com.cailiangzhe.lexidue.data.remote

import okhttp3.Interceptor
import java.io.IOException

internal const val MAX_DICTIONARY_RESPONSE_BYTES = 512L * 1_024L

internal class DictionaryResponseTooLargeException : IOException("Dictionary response exceeded the safe byte limit.")

internal fun dictionaryResponseSizeLimitInterceptor(maxBytes: Long = MAX_DICTIONARY_RESPONSE_BYTES): Interceptor {
    require(maxBytes > 0L) { "Response byte limit must be positive." }
    return Interceptor { chain ->
        val response = chain.proceed(chain.request())
        val body = response.body ?: return@Interceptor response
        val declaredLength = body.contentLength()
        if (declaredLength > maxBytes) {
            response.close()
            throw DictionaryResponseTooLargeException()
        }

        val preview = response.peekBody(maxBytes + 1L)
        if (preview.contentLength() > maxBytes) {
            response.close()
            throw DictionaryResponseTooLargeException()
        }
        response
    }
}
