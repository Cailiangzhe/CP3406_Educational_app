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
