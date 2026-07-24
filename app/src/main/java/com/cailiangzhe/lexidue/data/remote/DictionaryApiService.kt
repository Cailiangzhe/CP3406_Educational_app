package com.cailiangzhe.lexidue.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

internal interface DictionaryApiService {
    @GET("api/v2/entries/en/{word}")
    suspend fun lookup(
        @Path("word") word: String,
    ): Response<List<DictionaryEntryDto>>
}
