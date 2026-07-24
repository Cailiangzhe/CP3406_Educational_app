# LexiDue Decision Log

This log records decisions and verification evidence as the assessment moves from plans to implementation. Dates use the repository's Singapore project time.

## 2026-07-20 - M1 foundation and navigation

### AGP 9-compatible build baseline

**Decision.** Keep Android Gradle Plugin 9.2.1 and use its built-in Kotlin integration, with Kotlin Compose/serialization plugins 2.2.10, KSP 2.3.9, and Hilt 2.59.2. Gradle runs on JDK 21 while application Java compatibility and the Kotlin JVM target are both 17.

**Reason.** These versions sync and compile together without adding the obsolete Kotlin Android plugin. KSP is used for Room and Hilt code generation, and the shared version catalog keeps each choice visible and reproducible. Java/Kotlin 17 provides a consistent application bytecode target while JDK 21 satisfies the configured Gradle daemon toolchain.

**Boundary.** The M1 dependency catalog prepares Room, DataStore, Retrofit, Kotlin serialization, OkHttp, MockWebServer, Compose testing, and coroutine testing. Their presence does not claim that M2/M3 persistence or API features are implemented.

### Adaptive, type-safe navigation

**Decision.** Use one Compose `NavHost` with serializable `HomeRoute`, `PracticeRoute(sessionId)`, `StatisticsRoute`, and `SettingsRoute` destinations. `NavigationSuiteScaffold` adapts the three top-level destinations between supported compact and expanded navigation presentations; entering Practice hides top-level navigation and system Back returns to Home.

**Reason.** Typed routes remove manually parsed route strings, while a pre-created session ID establishes the navigation contract that Room will fulfil in M2. Top-level navigation uses single-top and state-restoring behaviour, and the focused Practice destination does not compete with the main navigation controls.

**Evidence.** Instrumentation reaches Home, Practice, Statistics, and Settings and verifies the Practice-to-Home system Back path. Practice still renders an in-memory foundation state; session persistence and summary navigation remain M2 work.

### Hilt and network boundary

**Decision.** Add `@HiltAndroidApp` and `@AndroidEntryPoint` application/activity entry points plus a singleton `NetworkModule`. The module provides a tolerant Kotlin serialization `Json`, a plain `OkHttpClient`, and Retrofit configured with the HTTPS Free Dictionary API base URL.

**Reason.** Establishing constructor-injectable boundaries now prevents screens from constructing future repositories or network clients directly. Release logging, API DTOs, service calls, response validation, cache policy, and Room integration remain deferred to the appropriate later milestone.

### Backup and cleartext security baseline

**Decision.** Disable platform backup with `android:allowBackup=false`; disable cleartext with both `android:usesCleartextTraffic=false` and a network security configuration; reference explicit legacy `backup_rules.xml` and Android 12+ `data_extraction_rules.xml` exclusions.

**Reason.** Privacy defaults were established before learning history exists, so M2 storage cannot accidentally inherit cloud-backup or device-transfer behaviour. Both rule formats exclude root, files, databases, shared preferences, external storage, and device-protected equivalents where the format supports them.

**Evidence.** The generated release merged manifest contains `allowBackup=false`, `usesCleartextTraffic=false`, `fullBackupContent=@xml/backup_rules`, and `dataExtractionRules=@xml/data_extraction_rules`.

### AndroidX signature receiver permission

**Observation.** The app source manifest requests only the Android platform `android.permission.INTERNET` permission. Manifest merging also defines and uses `com.cailiangzhe.lexidue.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` with `protectionLevel=signature`.

**Interpretation.** AndroidX Core generates this app-scoped permission to protect dynamically registered receivers that are marked not exported on older Android versions. It grants no platform data capability, has no runtime prompt, and another app cannot use it unless signed with the same certificate. It is therefore documented separately instead of being misreported as a second user-facing platform permission.

### Automated M1 gate

Verification was completed on 2026-07-20:

| Command/evidence | Result |
| --- | --- |
| `.\gradlew.bat --no-daemon spotlessCheck testDebugUnitTest lintDebug` | Passed. The JVM type-safe route test passed; lint reported no blocking errors. This command is also configured in `.github/workflows/android-ci.yml`. |
| `.\gradlew.bat processReleaseMainManifest` | Passed. The generated release manifest was audited for permissions, cleartext, backup flags, and both referenced backup rule files. |
| `.\gradlew.bat connectedDebugAndroidTest` | Passed on the Pixel 10 Pro XL Android 17 emulator: 4 tests, 0 failures, 0 errors, 0 skipped. |

The four connected tests provide the following evidence:

1. `ApplicationIdentityTest` verifies the `com.cailiangzhe.lexidue` application ID.
2. `HomeAccessibilityTest` renders Home at 200% font scale and verifies a heading semantic plus a displayed Start Practice target at least 48 dp high.
3. `LexiDueNavigationTest.requiredTopLevelScreens_areReachable` verifies Home, Statistics, and Settings top-level navigation.
4. `LexiDueNavigationTest.practiceSystemBack_returnsToHome` verifies Practice is reachable and system Back returns to Home.

**M1 conclusion.** The foundation/navigation gate is complete. This is not evidence for M2 learning logic, Room persistence, API enrichment, or real statistics; those milestones remain unchecked in the README.

## 2026-07-22 - M2 local learning vertical slice

### Room v1 and the canonical starter deck

**Decision.** Make Room the authoritative source for local learning state and export schema v1 with six entities: `WordEntity`, `CanonicalMeaningEntity`, `ReviewProgressEntity`, `PracticeSessionEntity`, `SessionQuestionEntity`, and `AttemptEntity`. Seed it with 24 original plain-language academic-word meanings split evenly across verbs, nouns, and adjectives.

**Reason.** A bundled, reviewed answer key makes first-run practice independent of network availability and prevents unreviewed API text from silently becoming scored content. The importer is one atomic transaction and uses stable IDs plus insert-ignore behaviour, so repeated application starts do not duplicate words, meanings, or progress rows.

**Boundary.** `ApiSenseEntity`, content-rights validation, an explicit schema migration, and DataStore preferences are intentionally absent from v1 and remain M3 work. The existing backup/data-extraction rules exclude the M2 database, and `android:allowBackup=false` remains unchanged.

### Authoritative persisted session state

**Decision.** Create the complete session before navigation and persist its ID, stable random seed, difficulty, ordered questions, prompts, option IDs, correct option IDs, retry relationship, current-question pointer, status, and timestamps. `SavedStateHandle` carries only `sessionId`; Practice and Summary reconstruct their state through repositories backed by Room.

**Reason.** A UI-only question list would be lost after process death and could regenerate different distractors. Storing the generated order and current pointer lets an unanswered question or already-recorded feedback return exactly as the learner left it.

**Evidence.** The Room test recreates a session snapshot after an idempotent answer write. ViewModel tests separately reconstruct unanswered and graded states, while the connected complete-session test uses the production Hilt/Room path from Home through Summary.

### Idempotent transactional learning writes

**Decision.** Enforce one attempt per question with a unique database index. Record the attempt, review update, session correct count, and optional retry question in one transaction. Advance with a compare-and-set operation against the expected current question; advancing the final question clears the pointer and completes the session once.

**Reason.** Compose recomposition, double taps, repeated effects, or retried events must not double-count an answer or skip a question. Keeping persisted feedback selected until an explicit successful Continue also makes restoration unambiguous.

**Boundary.** Skip is stored as `SKIPPED` but does not change review progress or accuracy. Exit records no attempt; it changes only an active session to `ABANDONED`, preserving prior answers.

### Deterministic questions, review, and delayed retry

**Decision.** Select due words first, fill with unique deck words, use a saved seed, alternate word-to-definition and definition-to-word questions, and generate unique distractors from the same part of speech. Foundation uses three options; Standard and Challenge use four. Correct answers advance through 1, 3, 7, 14, and 30-day intervals; incorrect answers reset to the first interval.

**Reason.** Seeded generation makes failures reproducible and same-part-of-speech distractors avoid trivial grammatical cues. Overflow-safe timestamp arithmetic and an injected time source keep review behaviour deterministic in tests.

**Delayed-retry boundary.** A retry must follow at least two intervening questions and cannot create another retry. The active Practice flow adds one only when at least two unanswered questions remain; an incorrect answer among the final two base questions is still saved and scheduled for spaced review but is not retried in that session.

### M2 UI, navigation, and accessibility boundary

**Decision.** Replace the Home and Practice sample path with Hilt ViewModels and add a Room-derived Practice Summary. Feedback combines text with a non-colour symbol and uses a polite live region; major actions and answer choices have 48 dp minimum targets; Practice and Summary remain vertically scrollable at 200% font scale. Back opens a confirmation and does not score leaving as incorrect.

**Navigation finding.** Device testing exposed that using saved-state restoration when explicitly returning to Home could restore the just-left Practice/Summary destination. Home navigation now discards the focused learning flow, while Statistics/Settings top-level navigation retains state restoration.

**Resilience finding.** A final code audit found that non-validation storage failures could escape Practice coroutines and that Home's unwired review metric would always display zero. Practice now retains a recoverable error state and can restart its Room observation through **Try again**; Home omits the misleading metric until M4 supplies the complete review query/UI binding.

**Boundary.** Automated M2 checks cover headings, 200% font, minimum targets, feedback announcement semantics, and colour-independent feedback. The manual TalkBack order, focus restoration, keyboard/D-pad/Switch Access, contrast, tablet, and orientation matrix remains M5 work. Statistics and Settings remain UI foundations; their live binding, persistence, and reset path are not M2 claims.

### Automated M2 gate

Verification was completed on 2026-07-22:

| Command/evidence | Result |
| --- | --- |
| `.\gradlew.bat --no-daemon spotlessCheck testDebugUnitTest lintDebug assembleRelease` | Passed. Spotless reported no formatting violations, all 41 JVM tests passed, Android Lint had no errors, and the release APK assembled. Remaining lint notices are dependency/SDK update suggestions rather than M2 defects. |
| `.\gradlew.bat connectedDebugAndroidTest` | Passed on the Pixel 10 Pro XL Android 17 emulator: 10 tests, 0 failures, 0 errors, 0 skipped. |

The 41 JVM tests cover starter-deck invariants, scoring, compatible distractors, deterministic generation, delayed retry, review scheduling, summary calculations, session creation, Home double-tap protection, Practice reconstruction/actions and recoverable storage failure, Summary derivation, and typed routes. The 10 device tests cover application identity, two Room transaction/reconstruction cases, Home/Practice/Summary accessibility, top-level navigation, confirmed abandonment, and a complete saved local session.

**M2 conclusion.** The offline local learning gate is complete. These results do not claim a live dictionary request, API cache/failure behaviour, DataStore settings, a Room-bound Statistics screen, local reset, migration coverage, or the final manual accessibility matrix; those remain M3-M5 work.

## 2026-07-23 - M3 API enrichment and offline-first persistence

### External source and content-rights boundary

**Decision.** Use the documented Free Dictionary API v2 English-entry endpoint only for optional, learner-triggered enrichment. One Home action may make at most five due-word network lookups; no background or whole-deck request exists. Show the provider and source in the saved UI, and disclose that the selected spelling plus ordinary connection metadata reaches the external service.

**Rights finding.** The provider describes the service as free and intended for learning applications, while its server repository is GPL-3.0. The reviewed pages do not separately grant response-data, caching, redistribution, or third-party audio rights. The complete finding and links are recorded in [`api-source-decision.md`](api-source-decision.md).

**Boundary.** The assessed build uses a narrow on-device cache, does not bundle or export API responses, uses only original synthetic test fixtures, and does not download or play audio. A public or commercial release requires explicit response/audio rights or a replacement provider. The server implementation's GPL licence is not treated as automatic permission to redistribute its output.

### Strict remote boundary and quarantined domain model

**Decision.** Put Retrofit behind a replaceable `DictionaryRemoteDataSource` and return exhaustive lookup results for success, invalid request, 404, other HTTP status, malformed payload, timeout, transport failure, and unusable content. Bound the full OkHttp call to 15 seconds and reject response bodies larger than 512 KiB before deserialisation. A success must match the exact normalized requested word and pass supported-part-of-speech, bounded-text, control/markup, optional-field, HTTPS URL, host-safety, stable-ID, deduplication, and maximum-sense checks.

**Reason.** A tolerant DTO is necessary because optional provider fields vary, but tolerance must not turn untrusted or mismatched content into saved learning material. Explicit outcomes let the repository preserve prior state without parsing exception messages. `ApiSense` is a separate display-only model and has no conversion into `CanonicalMeaning`, question answers, or distractors.

**Evidence.** Mapper and synthetic MockWebServer tests cover the exact GET path, tolerant success, omitted fields, 404, other HTTP status, malformed JSON, read/call timeout, `IOException`, oversized responses, mismatched words, unsupported parts of speech, unsafe/overlong text, protocol-relative-to-HTTPS audio normalization, unsafe optional URLs, stable deduplication, and bounded sense count. Home maps these detailed data-layer outcomes to a generic recoverable refresh failure instead of displaying transport internals. No automated test calls the live service.

### Room v2 migration and offline cache policy

**Decision.** Add `ApiSenseEntity` in schema v2 with a word foreign key, cascade delete, word/fetched-time indices, provider/source, optional metadata, and fetched timestamp. `ApiSenseDao.replaceForWord` requires a non-empty, same-word validated snapshot and replaces it transactionally. `MIGRATION_1_2` creates only the new table and indices; production registers the migration and has no destructive fallback.

**Cache policy.** `RoomDictionaryEnrichmentRepository` serializes refreshes, scans a bounded rotating window of local due candidates, makes at most five network lookups per action, skips entries that are no more than 30 days old without consuming that network budget, and writes only validated successes. Future timestamps are treated as stale so a device clock correction cannot make a cache entry permanently fresh. Rotation prevents fresh or repeatedly failing early candidates from permanently starving later due words. Any lookup failure leaves the previous cache untouched. Recent cached word relations are observed directly from Room, so Home can render saved content immediately after repository/database recreation without networking.

**Evidence.** Device tests verify cache/canonical isolation, atomic replacement, rejection without deletion, recent ordering, cascade behaviour, five-network-lookup bounding, the 30-day boundary, partial and total failure retention, starvation avoidance, and offline observation after a database reopen. The migration test inserts representative rows in every M2 table, migrates v1 to v2, validates the exported schema, and confirms all six original tables retain their data.

### Preferences DataStore and applied settings

**Decision.** Store only session length, difficulty, theme, sound, haptics, reduced motion, and onboarding state in Preferences DataStore. Unknown enum text or unsupported session lengths map to safe defaults. Corrupt files are replaced with defaults, and transient I/O read failures emit defaults before a delayed re-subscription so active Home/theme observers can recover. Relational learning history remains exclusively in Room.

**UI behaviour.** Settings uses a Hilt ViewModel with immutable loading/content/updating/error state, disables controls during writes, exposes retry after observation failure, and provides radio/switch semantics with minimum targets. The app shell applies safe-drawing insets to route content so headings remain below the status bar and cutout while edge-to-edge rendering stays enabled. Reset remains absent until M4 can provide a confirmed, tested deletion path. Home waits for settings before enabling session creation; persisted length and difficulty feed `StartPracticeSession`, while the root applies System/Light/Dark theme changes.

**Boundary.** Sound, haptics, reduced motion, and onboarding state persist, but M3 does not claim effects the current learning UI does not use. The core activity has no audio dependency, forced motion, or haptic requirement.

### Automated M3 gate

Verification was completed on 2026-07-23:

| Command/evidence | Result |
| --- | --- |
| `.\gradlew.bat --no-daemon testDebugUnitTest` | Passed: 79 tests, 0 failures, 0 errors, 0 skipped. |
| `.\gradlew.bat --no-daemon spotlessCheck` | Passed with no formatting violations. |
| `.\gradlew.bat --no-daemon --no-parallel assembleRelease` | Passed and produced `app-release-unsigned.apk`. |
| `.\gradlew.bat --no-daemon --no-parallel lintDebug` | Passed. An earlier invocation that ran debug lint concurrently with release Hilt generation hit an Android Lint missing-temporary-file worker error; sequential rerun passed without source changes for lint findings. |
| `.\gradlew.bat --no-daemon --no-parallel connectedDebugAndroidTest` | Passed on the Pixel 10 Pro XL Android 17 emulator: 21 tests, 0 failures, 0 errors, 0 skipped. |

**M3 conclusion.** The API enrichment/offline-persistence gate is complete: Home exposes the learner-triggered path and renders saved result/source/time/fallback state; strict response validation precedes the Room write; cache survives repository recreation while offline; failures preserve both cache and canonical practice; and persisted settings affect session creation and theme. Statistics screen binding, confirmed reset, final manual accessibility/responsive evidence, screenshots, and public-release content-rights confirmation remain M4-M6 work.
