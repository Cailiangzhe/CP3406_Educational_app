package com.cailiangzhe.lexidue.data.remote

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okio.Buffer
import okio.GzipSink
import okio.buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class DictionaryRemoteDataSourceTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `GETs normalized word and maps a tolerant success payload`() =
        runTest {
            enqueueJson(
                body =
                    """
                    [
                      {
                        "word": "analyse",
                        "phonetics": [
                          {
                            "text": "/əˈnælɪz/",
                            "audio": "//ssl.gstatic.com/dictionary/static/analyse.mp3",
                            "unneeded": "ignored"
                          }
                        ],
                        "meanings": [
                          {
                            "partOfSpeech": "verb",
                            "definitions": [
                              {
                                "definition": "Examine methodically and in detail.",
                                "example": "Researchers analyse the evidence.",
                                "synonyms": ["study"]
                              }
                            ]
                          }
                        ],
                        "sourceUrls": ["https://en.wiktionary.org/wiki/analyse"]
                      }
                    ]
                    """.trimIndent(),
            )

            val result = createDataSource().lookup(" Analyse ")

            assertTrue(result is DictionaryLookupResult.Success)
            result as DictionaryLookupResult.Success
            assertEquals("analyse", result.enrichment.normalizedWord)
            assertEquals(
                "Researchers analyse the evidence.",
                result.enrichment.senses
                    .single()
                    .example,
            )
            assertEquals(
                "https://ssl.gstatic.com/dictionary/static/analyse.mp3",
                result.enrichment.senses
                    .single()
                    .audioUrl,
            )
            assertEquals("/api/v2/entries/en/analyse", server.takeRequest().target)
        }

    @Test
    fun `omitted optional fields remain absent without failing the lookup`() =
        runTest {
            enqueueJson(
                body =
                    """
                    [
                      {
                        "word": "analyse",
                        "meanings": [
                          {
                            "partOfSpeech": "verb",
                            "definitions": [{"definition": "Examine in detail."}]
                          }
                        ]
                      }
                    ]
                    """.trimIndent(),
            )

            val result = createDataSource().lookup("analyse") as DictionaryLookupResult.Success
            val sense = result.enrichment.senses.single()

            assertNull(sense.example)
            assertNull(sense.phonetic)
            assertNull(sense.audioUrl)
        }

    @Test
    fun `maps HTTP 404 without trying to decode its error body`() =
        runTest {
            enqueueJson(code = 404, body = """{"title":"No Definitions Found"}""")

            assertSame(DictionaryLookupResult.NotFound, createDataSource().lookup("analyse"))
        }

    @Test
    fun `maps malformed JSON separately from transport failures`() =
        runTest {
            enqueueJson(body = """[{"word":"analyse",]""")

            assertSame(DictionaryLookupResult.MalformedPayload, createDataSource().lookup("analyse"))
        }

    @Test
    fun `maps a read timeout explicitly`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .addHeader("Content-Type", "application/json")
                    .headersDelay(250, TimeUnit.MILLISECONDS)
                    .body("[]")
                    .build(),
            )
            val timeoutClient =
                OkHttpClient
                    .Builder()
                    .readTimeout(50, TimeUnit.MILLISECONDS)
                    .callTimeout(1, TimeUnit.SECONDS)
                    .build()

            assertSame(
                DictionaryLookupResult.Timeout,
                createDataSource(timeoutClient).lookup("analyse"),
            )
        }

    @Test
    fun `maps the total call deadline as a timeout`() =
        runTest {
            server.enqueue(
                MockResponse
                    .Builder()
                    .addHeader("Content-Type", "application/json")
                    .headersDelay(250, TimeUnit.MILLISECONDS)
                    .body("[]")
                    .build(),
            )
            val timeoutClient =
                OkHttpClient
                    .Builder()
                    .callTimeout(50, TimeUnit.MILLISECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()

            assertSame(
                DictionaryLookupResult.Timeout,
                createDataSource(timeoutClient).lookup("analyse"),
            )
        }

    @Test
    fun `rejects an oversized response before decoding it`() =
        runTest {
            enqueueJson(
                body =
                    """
                    [
                      {
                        "word": "analyse",
                        "padding": "${"x".repeat(512)}",
                        "meanings": [
                          {
                            "partOfSpeech": "verb",
                            "definitions": [{"definition": "Examine in detail."}]
                          }
                        ]
                      }
                    ]
                    """.trimIndent(),
            )
            val sizeLimitedClient =
                OkHttpClient
                    .Builder()
                    .addInterceptor(dictionaryResponseSizeLimitInterceptor(maxBytes = 256L))
                    .build()

            assertSame(
                DictionaryLookupResult.UnusableContent,
                createDataSource(sizeLimitedClient).lookup("analyse"),
            )
        }

    @Test
    fun `rejects an oversized response after transparent gzip decoding`() =
        runTest {
            val uncompressedBody =
                """
                [
                  {
                    "word": "analyse",
                    "padding": "${"x".repeat(4_096)}",
                    "meanings": [
                      {
                        "partOfSpeech": "verb",
                        "definitions": [{"definition": "Examine in detail."}]
                      }
                    ]
                  }
                ]
                """.trimIndent()
            val compressedBody = gzip(uncompressedBody)
            assertTrue(compressedBody.size < 256L)
            server.enqueue(
                MockResponse
                    .Builder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Content-Encoding", "gzip")
                    .body(compressedBody)
                    .build(),
            )
            val sizeLimitedClient =
                OkHttpClient
                    .Builder()
                    .addInterceptor(dictionaryResponseSizeLimitInterceptor(maxBytes = 256L))
                    .build()

            assertSame(
                DictionaryLookupResult.UnusableContent,
                createDataSource(sizeLimitedClient).lookup("analyse"),
            )
        }

    @Test
    fun `maps non-timeout IOExceptions as network failures`() =
        runTest {
            val throwingApi =
                object : DictionaryApiService {
                    override suspend fun lookup(word: String): Response<List<DictionaryEntryDto>> = throw IOException("offline")
                }

            assertSame(
                DictionaryLookupResult.NetworkFailure,
                RetrofitDictionaryRemoteDataSource(throwingApi).lookup("analyse"),
            )
        }

    @Test
    fun `maps valid but unusable content separately`() =
        runTest {
            enqueueJson(body = "[]")

            assertSame(DictionaryLookupResult.UnusableContent, createDataSource().lookup("analyse"))
        }

    @Test
    fun `preserves non-404 HTTP status for caller policy`() =
        runTest {
            enqueueJson(code = 503, body = "{}")

            assertEquals(
                DictionaryLookupResult.HttpFailure(503),
                createDataSource().lookup("analyse"),
            )
        }

    @Test
    fun `invalid request is rejected without an HTTP call`() =
        runTest {
            assertSame(DictionaryLookupResult.InvalidRequest, createDataSource().lookup("en:analyse"))
            assertEquals(0, server.requestCount)
        }

    private fun enqueueJson(
        code: Int = 200,
        body: String,
    ) {
        server.enqueue(
            MockResponse
                .Builder()
                .code(code)
                .addHeader("Content-Type", "application/json")
                .body(body)
                .build(),
        )
    }

    private fun createDataSource(client: OkHttpClient = OkHttpClient.Builder().build()): DictionaryRemoteDataSource {
        val json =
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            }
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        return RetrofitDictionaryRemoteDataSource(retrofit.create(DictionaryApiService::class.java))
    }

    private fun gzip(body: String): Buffer {
        val compressed = Buffer()
        GzipSink(compressed).buffer().use { sink ->
            sink.writeUtf8(body)
        }
        return compressed
    }
}
