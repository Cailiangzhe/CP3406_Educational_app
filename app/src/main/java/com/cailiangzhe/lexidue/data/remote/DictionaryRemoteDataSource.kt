package com.cailiangzhe.lexidue.data.remote

import kotlinx.serialization.SerializationException
import java.io.IOException
import java.io.InterruptedIOException

interface DictionaryRemoteDataSource {
    suspend fun lookup(requestedWord: String): DictionaryLookupResult
}

internal class RetrofitDictionaryRemoteDataSource(
    private val api: DictionaryApiService,
) : DictionaryRemoteDataSource {
    override suspend fun lookup(requestedWord: String): DictionaryLookupResult {
        val normalizedWord =
            DictionaryResponseMapper.normalizeRequestedWord(requestedWord)
                ?: return DictionaryLookupResult.InvalidRequest

        return try {
            val response = api.lookup(normalizedWord)
            when {
                response.code() == HTTP_NOT_FOUND -> DictionaryLookupResult.NotFound
                !response.isSuccessful -> DictionaryLookupResult.HttpFailure(response.code())
                response.body() == null -> DictionaryLookupResult.MalformedPayload
                else -> DictionaryResponseMapper.map(normalizedWord, response.body().orEmpty())
            }
        } catch (_: DictionaryResponseTooLargeException) {
            DictionaryLookupResult.UnusableContent
        } catch (_: InterruptedIOException) {
            DictionaryLookupResult.Timeout
        } catch (_: SerializationException) {
            DictionaryLookupResult.MalformedPayload
        } catch (_: IOException) {
            DictionaryLookupResult.NetworkFailure
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
