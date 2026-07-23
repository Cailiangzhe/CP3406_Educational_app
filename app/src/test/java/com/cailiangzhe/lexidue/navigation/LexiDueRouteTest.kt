package com.cailiangzhe.lexidue.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class LexiDueRouteTest {
    @Test
    fun practiceRoute_roundTripsSessionIdentifier() {
        val route = PracticeRoute(sessionId = "session-42")

        val encoded = Json.encodeToString(PracticeRoute.serializer(), route)
        val decoded = Json.decodeFromString(PracticeRoute.serializer(), encoded)

        assertEquals(route, decoded)
    }

    @Test
    fun practiceSummaryRoute_roundTripsSessionIdentifier() {
        val route = PracticeSummaryRoute(sessionId = "session-42")

        val encoded = Json.encodeToString(PracticeSummaryRoute.serializer(), route)
        val decoded = Json.decodeFromString(PracticeSummaryRoute.serializer(), encoded)

        assertEquals(route, decoded)
    }
}
