package com.cailiangzhe.lexidue.navigation

import kotlinx.serialization.Serializable

/** Type-safe destinations used by the app's single navigation graph. */
@Serializable
data object HomeRoute

@Serializable
data object StatisticsRoute

@Serializable
data object SettingsRoute

/** A session identifier is created before navigation and will become the Room lookup key in M2. */
@Serializable
data class PracticeRoute(
    val sessionId: String,
)

/** A completed or restored session summary, backed by the same Room session identifier. */
@Serializable
data class PracticeSummaryRoute(
    val sessionId: String,
)
