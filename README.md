# LexiDue

> An offline-first academic vocabulary trainer for first-year university students aged 18-24 who use English as an additional language.

**Status:** M3 complete - API enrichment and offline-first persistence (2026-07-23)

**Course:** JCU CP3406 Assessment 3

**Platform:** Android, Kotlin, Jetpack Compose, Material Design 3

LexiDue has a runnable offline learning loop plus optional, learner-triggered dictionary enrichment. Home imports and observes the Room-backed starter deck, starts a saved deterministic session, and opens the real Practice flow. Answers, skips, delayed retries, review progress, the current question, completion, abandonment, and the session summary are reconstructed from Room rather than held only in UI memory.

M3 adds a validated Free Dictionary API path, a quarantined Room v2 cache, explicit v1-to-v2 migration, offline cache display, and Preferences DataStore settings. Persisted session length and difficulty affect new sessions, and the selected theme is applied at the app root. Statistics UI binding and the confirmed local-data reset remain M4 work; the final manual accessibility/responsive matrix remains M5.

## Current implementation evidence

- An atomic, idempotent importer seeds 24 original academic-word entries: eight verbs, eight nouns, and eight adjectives, each with one reviewed canonical meaning.
- Current Room schema v2 stores words, canonical meanings, review progress, practice sessions, ordered questions, one unique attempt per question, and separately quarantined API senses. Exported v1/v2 schemas remain under `app/schemas`.
- Session creation selects due words first, fills without duplicate words, stores a stable random seed, alternates both question modes, and persists prompts, option IDs, answer IDs, order, and the current-question pointer before navigation.
- Same-part-of-speech distractors are unique and seedable. Foundation generates three choices; Standard and Challenge generate four. Unsafe questions are rejected when compatible distractors are insufficient.
- Correct and incorrect answers update the attempt, review state, and session count in one Room transaction. A unique question index and compare-and-set advancement make repeated answer/Continue events harmless. Skip is saved but unscored; Exit marks an active session abandoned without recording an incorrect answer.
- Review intervals advance through 1, 3, 7, 14, and 30 days. An incorrect answer resets the card to the first interval and is retried only when at least two later questions can remain between the original and retry; retries do not recursively create more retries.
- Home, Practice, and Practice Summary use Hilt ViewModels with immutable state. Practice restores either an unanswered question or persisted feedback after recreation, and the summary derives accuracy, skip/retry counts, review words, and completion time from saved data.
- Home now makes at most five due-word network lookups per explicit action. Exact-word validation, supported-part-of-speech filtering, bounded safe text, HTTPS-only audio metadata, stable content IDs, and deterministic deduplication run before a successful response can reach Room.
- `ApiSenseEntity` stores provider content separately from canonical meanings. A 30-day freshness rule avoids automatic/reflexive requests, and every lookup failure leaves both the old cache and the reviewed quiz deck untouched.
- Room schema v2 and `MIGRATION_1_2` add only the enrichment table, its foreign key, and indices. Migration tests verify that all six M2 tables retain their rows. No destructive migration fallback is configured.
- Settings persist safe values for 5/10/15-word sessions, difficulty, theme, sound, haptics, reduced motion, and onboarding state. Unknown or damaged preference values fall back safely.
- The source/rights decision and release boundary are recorded in [`docs/api-source-decision.md`](docs/api-source-decision.md). Tests use original synthetic fixtures and never call the live service.
- Verification on 2026-07-23 passed 79 deterministic JVM tests and 21 Android instrumentation tests. Spotless, Android Lint, and the release build also pass.

## Product vision

Many new university students can recognise academic words while reading but struggle to recall their meaning, distinguish similar definitions, or use the words confidently. LexiDue will turn a curated academic vocabulary deck into short retrieval-practice sessions with immediate feedback, delayed retry, spaced review, and local progress statistics.

The app is deliberately narrower than a general dictionary. Its purpose is to help learners practise a controlled set of useful academic words, identify items that need review, and build durable recall without accounts, advertising, social comparison, or time pressure.

## Target learners and learning outcomes

**Primary audience:** first-year university students aged 18-24 who use English as an additional language.

The MVP will help a learner:

- recall the meaning of a curated academic word;
- identify the correct word from a definition;
- distinguish plausible but incorrect definitions;
- recognise the word's part of speech and optional phonetic form;
- revisit due words and other words selected for review through a transparent schedule; and
- use local statistics to choose the next useful practice session.

## Core learning loop

1. The learner starts a 5-, 10-, or 15-word session from the Home screen.
2. The app selects due words and other words marked for review from Room, falling back to the curated starter deck.
3. The Activity screen presents one cued-retrieval question at a time.
4. The learner receives immediate text feedback and a short explanation.
5. An incorrect word is re-queued after at least two different questions, avoiding instant memorisation of the previous answer.
6. Every answer is saved atomically; Room aggregate queries are ready for the live Statistics wiring scheduled for M4.
7. Correct answers advance the word through review intervals of 1, 3, 7, 14, and 30 days. An incorrect answer returns it to the first interval.

Skip and Exit are never scored as incorrect. Inactivity does not remove mastery or trigger negative feedback, and due dates are recommendations rather than penalties.

The MVP will use two scored question modes with seedable generation:

- **Definition to word:** select the word that matches a definition.
- **Word to definition:** select the correct meaning for a word.

Difficulty changes distractor quality rather than adding time pressure: Foundation uses three clearly distinct choices, Standard uses four part-of-speech-compatible choices, and Challenge uses four more semantically similar choices. All modes remain untimed.

Typed recall, cloze questions, and pronunciation audio are stretch goals. They will not delay the required four-screen app.

## Core screen plan

| Required screen | Learner goal | Planned content and behaviour | Acceptance evidence |
| --- | --- | --- | --- |
| **Home / Landing** | Understand what to practise and start quickly | App purpose, due/review word count, current persisted session length and difficulty, start button, explicit **Enrich deck** action (maximum five network lookups), compact progress summary, saved-content/last-refresh status, and navigation to Statistics and Settings | A learner can start a valid session in two actions; a marker can visibly demonstrate API request -> validation -> Room -> cached reuse; loading, empty, cached-content, and refresh-failure states are recoverable |
| **Activity** | Complete a focused learning session | One question at a time, progress indicator, large answer targets, immediate correct/incorrect feedback with icon and text, definition/part of speech, optional phonetics, delayed retry, pause/exit, and end-of-session summary | A session uses the canonical local deck, records every answer, survives configuration changes, and never relies on colour alone; enrichment remains reference-only |
| **Statistics** | Understand progress and choose the next practice focus | Total sessions, overall accuracy, mastered/due counts, review-box distribution, words to review, and recent sessions; every chart also has a text summary | Results persist after restart, update immediately after a session, and remain understandable with TalkBack or without colour |
| **Settings** | Control the experience and local data | Session length, difficulty, theme, sound/haptics, reduced motion, privacy/API information, and a confirmed reset/delete action | Preferences persist, the core activity still works when optional effects are disabled, and reset removes local learning history |

### Navigation plan

- `Home` is the start destination.
- `Statistics` and `Settings` are top-level destinations.
- `Practice(sessionId)` and `PracticeSummary(sessionId)` form a nested activity flow launched from Home.
- A Material 3 `NavigationSuiteScaffold` uses a navigation bar on compact widths and a navigation rail on expanded widths; the practice flow hides top-level navigation to preserve focus.
- Destinations use serializable route types rather than manual route strings.
- Starting a session creates its identifier before navigation. `SavedStateHandle` restores only `sessionId`; the ViewModel reconstructs the authoritative question order, attempts, and progress from Room.
- Back from an unanswered session asks whether to leave; recorded answers remain saved. Back from the summary returns to Home without duplicating destinations.

## Scope

### MVP

- Four complete Compose screens with Material 3 styling.
- Typed Navigation Compose destinations and tested back-stack behaviour.
- A curated starter deck and two multiple-choice question modes.
- A visible, learner-triggered Free Dictionary API enrichment flow with bounded cache refresh.
- Room persistence for dictionary content, sessions, attempts, and review progress.
- DataStore persistence for user preferences.
- Offline practice from local data after first-run seed import.
- Clear loading, content, empty, stale-cache, cached fallback, and refresh-failure states.
- Model, repository, ViewModel, Room, Compose UI, and navigation tests.
- Privacy, accessibility, and ethical-design checks described below.

### Stretch goals

- Pronunciation playback when the API supplies audio, always with a text/phonetic alternative.
- User-directed word lookup and a reviewed custom deck.
- Cloze questions only when a safe, unambiguous example sentence is available.
- Richer progress visualisations and deep links.

### Explicitly out of scope

- Accounts, profiles, a custom backend, or cloud progress sync.
- Advertising, analytics, leaderboards, social sharing, or competitive ranking.
- Chat, user-generated public content, or unreviewed feeds.
- Speech recognition, microphone use, OCR, camera/location/storage access, or media upload.
- Push notifications, coercive streaks, countdown pressure, loot-box rewards, or guilt-based copy.
- Premature multi-module architecture; one app module with clear feature packages is sufficient.

## Architecture

LexiDue uses a single-activity, layered MVVM architecture with feature-grouped UI and unidirectional data flow. Screen-level ViewModels expose immutable `StateFlow<UiState>` values and accept explicit UI actions. Composables render state and never call an API, DAO, or DataStore directly. Every edge below is implemented.

```mermaid
flowchart TD
    UI["Compose screens"] -- "UiAction" --> VM["Screen ViewModels"]
    VM -- "immutable UiState" --> UI
    VM --> UC["Quiz and review use cases"]
    VM --> WR["WordRepository"]
    VM --> PSR["PracticeSessionRepository"]
    VM --> STAT["StatisticsRepository: M2 data, M4 screen binding"]
    VM --> DER["DictionaryEnrichmentRepository"]
    VM --> SR["SettingsRepository"]
    UC --> WR
    UC --> PSR
    WR --> DB["Room: canonical source of truth"]
    PSR --> DB
    STAT --> DB
    DER -- "select due candidates" --> WR
    DER --> DB
    DER -- "explicit, max 5 stale words" --> API["Free Dictionary API"]
    SR --> SETTINGS["DataStore preferences"]
```

### Package structure

```text
com.cailiangzhe.lexidue
|-- MainActivity.kt      # Single activity and top-level app state
|-- navigation/          # Serializable destinations and NavHost
|-- core/designsystem/   # Theme tokens and reusable accessible components
|-- data/
|   |-- local/           # Room entities, DAOs, database and starter deck
|   |-- preferences/     # Preferences DataStore mapping and Hilt binding
|   |-- remote/          # Retrofit service, DTOs, validation and result mapping
|   `-- repository/      # Offline-first repository implementations
|-- domain/
|   |-- model/           # Shared learning and persistence models
|   |-- repository/      # Repository contracts
|   `-- usecase/         # Session generation, scoring, review scheduling
|-- feature/
|   |-- home/
|   |-- practice/
|   |-- statistics/
|   `-- settings/
`-- di/                  # Hilt modules
```

### Android components and milestone status

- **UI:** Jetpack Compose and Material Design 3.
- **Navigation:** Navigation Compose with serializable, type-safe destinations.
- **State:** ViewModel, coroutines, `StateFlow`, immutable UI state, and lifecycle-aware collection.
- **Dependency injection:** Hilt with constructor-injected repositories, DAOs, remote data source, DataStore, minSdk-compatible `TimeProvider`, and ID/random providers.
- **Networking:** Retrofit, OkHttp, and tolerant Kotlin serialization feed a replaceable `DictionaryRemoteDataSource` with explicit success/failure results.
- **Persistence:** Room is the source of truth for relational learning/cache data; Preferences DataStore owns only simple user settings.
- **Code quality:** Spotless with ktlint plus Android Lint; both run locally and in CI.
- **Background work:** not required for the MVP; refresh remains learner-initiated and transparent.

## API and offline-first data flow

The optional external source is the key-free [Free Dictionary API](https://dictionaryapi.dev/), using:

```text
GET https://api.dictionaryapi.dev/api/v2/entries/en/{word}
```

LexiDue maps definitions, supported parts of speech, phonetics, examples, and optional audio URL metadata. It requests only curated English words. The application payload contains the lookup word only; it never contains answers, scores, identifiers, settings, or progress. As with any internet request, the provider can still receive ordinary connection metadata such as the device IP address and User-Agent. Home discloses this before refresh, and release builds do not add request/body logging. OkHttp bounds each complete call to 15 seconds and rejects a response body larger than 512 KiB before Retrofit deserialises it.

Room remains the canonical source for learning content and progress. The implemented flow is:

1. **Implemented in M2:** an atomic/idempotent first-run import stores 24 original plain-language canonical meanings: eight verbs, eight nouns, and eight adjectives.
2. **Implemented in M2:** Home and Practice observe/read Room and a complete session remains usable without a connection.
3. **Implemented in M3:** Home makes at most five due-word network lookups per tap and never bulk-fetches or refreshes in the background. The repository scans a bounded rotating candidate window so fresh or repeatedly failing early entries do not permanently starve later due words. A cached word remains fresh through 30 days and is eligible again only after that boundary.
4. **Implemented in M3:** exact normalized-word matching, a part-of-speech allow-list, text bounds/control-character checks, safe optional-field handling, HTTPS audio URL validation, stable hashing, and deduplication run before persistence.
5. **Implemented in M3:** validated senses, examples, phonetics, and audio URL metadata are stored separately from canonical quiz meanings and cannot replace an answer key or enter the distractor pool.
6. **Implemented in M3:** timeout, 404, malformed payload, oversized response, transport failure, other HTTP failure, or no usable definition returns a distinct data-layer result. Home deliberately presents refresh problems as one recoverable learner-facing failure rather than exposing transport details. Existing cache rows are not deleted on failure.
7. **Implemented in M3:** Home shows provider/source, the network-to-validation-to-Room path, refresh outcome, last successful time, fresh/stale status, and cached enrichment after repository/app recreation without networking.

The [source decision](docs/api-source-decision.md) records the provider's stated learning-app purpose, GPL-3.0 server repository, and the unresolved response-content/audio-rights boundary. This assessed build therefore uses learner-triggered, bounded, on-device caching with visible attribution and no audio playback or redistribution. A public/commercial release requires explicit rights confirmation or a replacement source; server-code licensing alone is not treated as permission to redistribute returned content.

No test depends on the live API. Original synthetic MockWebServer fixtures verify Retrofit/serialization and HTTP failures; fakes and in-memory Room keep repository and ViewModel tests deterministic.

### Room v2 model

Room schema v2 keeps the six M2 entities and adds the quarantined `ApiSenseEntity` cache.

| Entity | Minimum stored fields | Purpose |
| --- | --- | --- |
| `WordEntity` | normalized key such as `en:analyse`, display spelling, deck/source metadata | Stable word identity; the remote API supplies no database ID |
| `CanonicalMeaningEntity` | local stable ID, word key, part of speech, original definition, reference/provenance | Reviewed local meaning used for question answers and distractors |
| `ApiSenseEntity` | local stable ID/hash, word key, part of speech, definition, optional example/phonetic/audio URL, provider/source, fetched timestamp | Quarantined cached enrichment, separate from the scored quiz pool |
| `ReviewProgressEntity` | word ID, review box, correct/incorrect counts, next review timestamp | Transparent spaced-review state |
| `PracticeSessionEntity` | local UUID, difficulty, random seed, planned word count, status (`ACTIVE`, `COMPLETED`, `ABANDONED`), correct count, current-question ID, start time, nullable end time | Authoritative session lifecycle and current pointer; delayed retries do not change the planned word count |
| `SessionQuestionEntity` | local UUID, session ID, sequence, word key, question type, stored option IDs, optional retry-of ID | Reconstructs the exact active session after process death |
| `AttemptEntity` | local UUID, unique question-instance ID, session ID, outcome (`CORRECT`, `INCORRECT`, `SKIPPED`), retry flag, answer timestamp | Idempotent answer record; uniqueness prevents double-tap/recomposition double counting |

Room evidence includes exported v1/v2 schemas, foreign keys and indices, word/meaning/enrichment and session/question/attempt relations, atomic starter/session/cache replacement transactions, a unique attempt per question, compare-and-set question advancement, and `Flow` cache/due/statistics queries. `MIGRATION_1_2` creates only `api_senses` and its indices; its device test verifies every M2 table survives. No destructive fallback is configured.

Implemented learning rules define **due** as `nextReviewAt <= TimeProvider.now`, **mastered** as review box 5, and **words to review** as at least two graded attempts plus accuracy below 60%, or an incorrect most-recent graded attempt. `SKIPPED` outcomes are excluded from accuracy and mastery calculations. M2 includes the pure calculations and aggregate DAO/repository boundary; full Statistics screen binding remains M4.

DataStore contains only session length, difficulty, theme, sound/haptics, reduced motion, and onboarding state. It validates stored values and supplies safe defaults. Session length/difficulty affect session creation, and theme changes apply at the app root; relational learning progress is not duplicated in DataStore.

## Ethical and professional design commitments

### Privacy and data minimisation

- No account, real name, email, advertising ID, analytics, location, contacts, or device identifier.
- The only Android platform permission requested by the app is `android.permission.INTERNET`; it has no runtime prompt. AndroidX also generates and uses the app-scoped `com.cailiangzhe.lexidue.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, a `signature`-level custom permission that protects non-exported dynamic receivers. It is not a platform data-access permission and cannot be granted to an app signed with another key.
- Dictionary traffic uses HTTPS with cleartext traffic disabled. Any future permission requires a dated justification, just-in-time explanation, safe denial path, and README update.
- Learning history stays on the device and is never uploaded.
- M4 will add a clear confirmed reset action that deletes attempts, sessions, and review progress.
- The privacy baseline sets `android:allowBackup=false`, references a legacy `full-backup-content` rule and Android 12+ `data-extraction-rules`, and excludes every supported storage domain from both cloud backup and device transfer. The release merged manifest was verified before M2 progress data was introduced, and the M2 Room database remains covered by those exclusions. History currently remains until uninstall; after M4 it will also be removable through Reset.
- Keys, tokens, local configuration, and generated build outputs remain excluded from Git; exported Room schema history is intentionally tracked.

### Safe and age-appropriate learning design

- Automatic practice uses only a curated academic word allow-list.
- The source/rights review, narrow coursework cache policy, attribution, and public-release gate are recorded in [`docs/api-source-decision.md`](docs/api-source-decision.md).
- Fetched content is not automatically trusted: empty, malformed, excessively long, control/markup-bearing, word-mismatched, unsupported-part-of-speech, or unsafe-URL fields are rejected or omitted. Every remote sense remains outside the quiz pool.
- Feedback is calm and specific: it explains the answer without shame or exaggerated celebration.
- Mastery and due-review information replace streak-loss pressure and social comparison.
- Sessions are untimed; learners may skip or stop without penalty. Session-length and difficulty controls now persist and affect newly created sessions.
- Skip/Exit never lowers a score or mastery value, and review reminders are neutral recommendations.
- Rewards never block content, hide progress, or use variable-reward mechanics.

### Accessibility and inclusion

- Meaningful TalkBack labels, headings, roles, state descriptions, and logical focus order.
- Minimum 48 dp interactive targets and sufficient spacing.
- Support for large text, including a 200% font-scale check with no clipping.
- Contrast targets of at least 4.5:1 for normal text and 3:1 for large text and essential UI graphics; correctness is communicated with icon, text, and colour together.
- Text summaries for every progress chart.
- Polite live-region announcements for answer feedback and network errors. Automated focus restoration after question changes and dialogs remains part of the M5 accessibility work.
- Visible, logical focus for keyboard, D-pad, and Switch Access interaction will be included in the M5 manual matrix.
- M3 may retain a validated HTTPS audio URL as metadata only; it neither downloads nor plays third-party audio. Any future playback must remain optional, learner-triggered, and paired with phonetic/text information.
- Reduced-motion and sound/haptic preferences persist; core learning currently depends on neither animation nor audio.
- Responsive layouts will be checked on compact and expanded widths, portrait and landscape during M5.
- Plain English instructions and no accent-based scoring.

## Testing strategy

| Layer | Checks | Current evidence and remaining work |
| --- | --- | --- |
| Pure model/use-case tests | scoring, unique distractors, delayed retry, 1/3/7/14/30-day schedule, mastery calculation, user-setting invariants, injected time and seeded random | Deterministic JVM coverage through M3 |
| HTTP/mapping tests | Retrofit success, exact request path, 404/other HTTP, malformed JSON, timeout/IOException, optional omission, validation/deduplication, safe audio URL handling | Synthetic MockWebServer/mapper tests pass; no test calls the live API |
| Repository tests | starter/session persistence, preference validation/recovery, bounded refresh, 30-day freshness, partial/total failure retention, and offline observation after repository/database recreation | Fakes, Preferences DataStore tests, and Room device tests cover local and enrichment paths |
| ViewModel tests | loading, reconstruction, feedback, retry, completion, summary, recoverable settings/enrichment errors, setting updates, and double taps | Home, Practice, Summary, and Settings coroutine tests pass |
| Room tests | relations, indices, transactions, cascade behaviour, due/review/cache queries, aggregates, and v1-to-v2 migration | Device tests verify cache isolation/replacement, all M2 tables after migration, and saved-session reconstruction |
| Compose UI tests | answer feedback, semantics, large targets, large font, and reachable Settings controls | Automated semantics/200% font coverage passes; preference persistence is verified below the UI, while reset confirmation and the final manual matrix remain M4/M5 |
| Navigation tests | Home to Practice to Summary, Back/abandon behaviour, top-level screens and session arguments | Implemented with route JVM tests and a complete Room-backed device journey |
| Manual accessibility/responsive pass | TalkBack order and announcements, focus restoration, keyboard/D-pad/Switch Access focus, 200% font, measurable contrast, colour-independent feedback, reduced motion, phone/tablet, portrait/landscape | Final checklist and screenshot matrix |

Quality commands (the first command is also the CI verification gate):

```bash
./gradlew spotlessCheck testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest  # requires a running emulator/device
```

Windows PowerShell:

```powershell
.\gradlew.bat spotlessCheck testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest  # requires a running emulator/device
```

## Rubric-to-evidence plan

| Criterion | Weight | Planned excellent-band evidence |
| --- | ---: | --- |
| General code quality | 10% | Layered packages with feature-grouped UI, clear Kotlin naming, small single-purpose classes/composables, decision-focused comments, no sample/dead code, Spotless/ktlint and Android Lint checks |
| Design and UI | 10% | Material 3 tokens/components, complete loading/empty/offline/error/content states, responsive layouts, accessibility checklist, final screenshots |
| Navigation | 15% | One tested NavHost, serializable destinations, nested Practice/Summary flow, state restoration, correct back-stack behaviour |
| App architecture | 15% | Single-activity MVVM/UDF, immutable UI state, ViewModels, Hilt, repository contracts, use cases, Room source of truth, no API/DAO calls from UI |
| Advanced API features | 20% | Dictionary API mapping plus visible error handling; Room relations, transactions, aggregate Flow queries, restart-safe progress, and offline cache demonstration |
| Unit testing | 10% | Model and data unit tests plus Room, ViewModel, Compose GUI, and navigation tests covering success, failure, and offline paths |
| GitHub and version control | 10% | Small rubric-aligned commits, milestone issues/checklists, evolving README, no secrets or generated build outputs, tracked Room schema history, green checks on `main`, screenshots and test evidence |
| Self-reflection | 10% | Dated decision evidence linking Assessment 2 ethics and ACS principles to implementation, verification, trade-offs, and all six Gibbs stages |

## Development milestones

- [x] **M0 - Repository baseline and plan**
  - Android Studio Compose scaffold, GitHub repository, rubric-aligned README, and passing baseline unit task.

- [x] **M1 - Foundation and navigation**
  - Rename namespace/application ID and source/test packages to `com.cailiangzhe.lexidue`.
  - Add version-catalog entries for Kotlin serialization, KSP, Hilt, Navigation Compose, lifecycle Compose, Room, DataStore, Retrofit/serialization/OkHttp, coroutine testing, Room testing, Compose testing, MockWebServer, and Spotless/ktlint.
  - Define theme tokens, Hilt setup, feature contracts, serializable routes, adaptive top-level navigation, four screen foundations, the backup/network-security baseline, and `.github/workflows/android-ci.yml`.
  - Gate passed on 2026-07-20: Home, Practice, Statistics, and Settings are reachable; system Back returns from Practice to Home; instrumentation verifies heading semantics, a minimum 48 dp action target, and Home at 200% font scale.
  - Verification passed: CI is configured to run `spotlessCheck`, `testDebugUnitTest`, and `lintDebug`; four connected instrumentation tests passed; the release merged manifest has `android:usesCleartextTraffic=false` and `android:allowBackup=false`, and references both backup-rule files. The only requested Android platform permission is `INTERNET`; the additional AndroidX permission is the app-scoped signature permission explained in the privacy section.

- [x] **M2 - Local learning vertical slice**
  - Added the 24-word canonical starter deck, atomic/idempotent importer, exported Room v1 schema, six core entities, DAOs, transactions, repository contracts and Hilt bindings.
  - Added due-first deterministic session creation, both question modes, compatible distractors, scoring, delayed retry, the 1/3/7/14/30-day schedule, Home/Practice/Summary ViewModels, and the complete accessible UI/navigation path.
  - Gate passed on 2026-07-22: Room prevents duplicate attempts and reconstructs the exact current question or feedback; a device test completes a real saved session and summary; 41 JVM and 10 Android instrumentation tests pass.

- [x] **M3 - API enrichment and offline-first persistence**
  - Added the Dictionary API service/data source, source-rights decision, synthetic MockWebServer fixtures, strict validation/mapping, quarantined `ApiSenseEntity`, enrichment relations/DAO/repository, bounded five-lookup refresh, 30-day freshness, and explicit Room v1-to-v2 migration.
  - Added Preferences DataStore with validation and safe defaults; Settings now persists session length, difficulty, theme, sound, haptics, and reduced motion, while Home/session creation and the app theme consume the relevant values.
  - Gate passed on 2026-07-23: Home visibly describes network -> safety checks -> local save and renders provider/cache/result/time states; repository recreation observes saved content with networking unavailable; all lookup failures preserve cache/canonical learning; 79 JVM and 21 connected tests pass.

- [ ] **M4 - Statistics, settings, and privacy controls**
  - Wire the existing Room aggregate queries into Statistics UI and implement a disclosed, confirmed, verified local reset. Extend preference effects only where the UI actually uses them.
  - Gate: statistics update immediately and survive restart; reset removes learning history while preserving safe defaults; new UI includes semantics, large targets, and dynamic-text checks.

- [ ] **M5 - UI, accessibility, and resilience polish**
  - Adaptive layouts, all screen states, TalkBack semantics/announcements/focus restoration, large text, measurable theme contrast, keyboard/D-pad/Switch Access focus, reduced motion, and usability review.
  - Gate: manual accessibility/responsive checklist and critical Compose tests pass.

- [ ] **M6 - Verification, documentation, and submission**
  - Full tests/lint, screenshots, architecture and data evidence, known limitations, README update, release build, project ZIP, and Gibbs reflection PDF.
  - Gate: a fresh clone builds, required checks pass, repository access is shared with teaching staff, and submission artifacts are ready.

## Git and documentation workflow

- Keep `main` reproducible and commit after each coherent piece of progress.
- Use focused commit messages, for example:
  - `feat(navigation): add typed app destinations`
  - `feat(data): cache dictionary entries in Room`
  - `test(practice): cover review scheduling and retry`
  - `docs(readme): add implemented screenshots and results`
- Avoid one large final code dump; the history should demonstrate continuous development.
- Update this README at every milestone so planned claims become implemented evidence.
- Add final screenshots only after the corresponding screen is functional.
- Update `docs/decision-log.md` at every milestone for architecture, API/content limitations, privacy, accessibility, autonomy, failed alternatives, feelings, and usability feedback.

## Gibbs reflection evidence plan

Development notes will record:

1. the exact Assessment 2 ethical issue and relevant ACS principle;
2. the initial assumption and contemporaneous feeling/reaction;
3. alternatives considered and the technical/design decision made;
4. the relevant implementation commit;
5. the verification result, screenshot, test, or usability observation;
6. what worked, what failed, and the limitation/trade-off; and
7. the next action.

Minimum evidence includes the merged-manifest and backup audit, reset test, API data-flow/content-review record, accessibility matrix, and safe persuasive-copy decision.

The final approximately 1000-word reflection will explicitly cover Description, Feelings, Evaluation, Analysis, Conclusion, and Action Plan rather than only summarising features.

## Definition of done

The assessment build is complete when:

- all four required screens are functional and reachable through modern navigation;
- a learner can complete a session from the canonical local deck while refreshed or cached enrichment remains available as optional reference content;
- every answer persists and statistics update correctly after restart;
- API loading, empty, cached-content, and generic refresh-failure states are visible and recoverable; data-layer tests separately distinguish 404, malformed, oversized, timeout, transport, and other HTTP results;
- privacy reset deletes local learning history and backup behaviour is intentional;
- phone/tablet, portrait/landscape, TalkBack, 200% font, contrast, and colour-independent feedback checks pass;
- model and GUI test suites plus lint pass;
- the README contains implemented screenshots, architecture, API attribution, privacy decisions, test evidence, and known limitations;
- Git history shows regular focused commits rather than a single final upload; and
- the Gibbs reflection uses concrete development and ethics evidence.

## Current project configuration

The repository currently uses:

- Android Gradle Plugin 9.2.1 with its built-in Kotlin integration;
- Kotlin Compose/serialization plugins 2.2.10, KSP 2.3.9, and Hilt 2.59.2;
- Java 17 source/bytecode compatibility and Kotlin JVM target 17;
- Jetpack Compose with the Compose BOM and Material 3 adaptive navigation suite;
- Navigation Compose 2.9.8 with serializable type-safe routes;
- `minSdk` 24, `targetSdk` 36, and compile SDK 36.1;
- Gradle daemon/toolchain JDK 21 (Android Studio's bundled JBR is suitable);
- Hilt application/activity entry points, an injectable Retrofit/OkHttp/serialization boundary, and replaceable remote dictionary data source;
- Room schema v2 with seven related entities, exported v1/v2 history, explicit migration, transactional DAOs, five repository boundaries, and a 24-word local starter deck;
- Preferences DataStore with validated settings, Hilt binding, persisted Settings UI, root theme application, and Home session configuration;
- seedable learning use cases for question generation, scoring, delayed retry, summaries, due-first session creation, and 1/3/7/14/30-day review scheduling;
- Room-backed Home, Practice, and Practice Summary ViewModels, DataStore-backed Settings, and the Statistics screen foundation;
- 79 JVM tests and 21 connected Android tests covering the M3 gate; and
- Spotless/ktlint, JUnit, Compose UI, navigation, and Android instrumentation test support.

M3 keeps practice deliberately independent of networking: canonical questions and all learning/session state are local. Dictionary enrichment is optional, bounded, validated, separately cached, and failure-tolerant. Live Statistics screen binding, confirmed reset, final manual accessibility/responsive evidence, and release-use confirmation for third-party response/audio rights remain later work.

## Getting started

1. Clone the repository.
2. Open it in Android Studio.
3. Select Android Studio's bundled JBR 21 as the Gradle JDK, then allow Gradle to sync and install the required Android SDK components.
4. Run the `app` configuration on an emulator or Android device.
5. Run the same formatting, JVM-test, and lint gate used by CI:

```bash
./gradlew --no-daemon spotlessCheck testDebugUnitTest lintDebug
```

6. With an emulator running or a device connected, run the Room, complete-session navigation, identity, and accessibility instrumentation tests:

```bash
./gradlew connectedDebugAndroidTest
```

Windows PowerShell uses `.\gradlew.bat --no-daemon spotlessCheck testDebugUnitTest lintDebug` and `.\gradlew.bat connectedDebugAndroidTest`. If a standalone terminal cannot find Java, set `JAVA_HOME` to the Android Studio `jbr` directory for that terminal session. The connected test task requires a running emulator or connected device and is intentionally separate from the current host-only GitHub Actions job.

`local.properties`, signing credentials, environment files, document-review artifacts, and generated build outputs are intentionally excluded from version control.

## Key risks and mitigations

| Risk | Mitigation |
| --- | --- |
| Dictionary API unavailable or incomplete | Ship an original canonical starter deck, read from Room first, retain cached enrichment, expose refresh-failure/saved-content state, and never depend on live API in tests |
| API response or audio rights are unclear | Keep the documented narrow coursework cache/no-playback policy; obtain explicit rights before public release or replace the source behind `DictionaryEnrichmentRepository` |
| Missing or ambiguous meanings/examples | Keep API senses outside the scored pool, use one canonical local meaning per question, generate distractors from compatible parts of speech, and skip unsafe questions |
| Distractor or review logic becomes nondeterministic | Inject `TimeProvider` and a seeded random provider; use invariant checks such as unique answer options |
| Scope grows beyond the schedule | Protect the four screens, two quiz modes, API/Room, architecture, and tests; defer audio, custom decks, charts, and deep links |
| Accessibility is left until the end | Build shared accessible components early and add semantics, large-text, contrast, and screen-state checks in each milestone |
| README overstates implementation | Keep the status section current and change planned statements only when evidence exists |

## Technical references

- [Free Dictionary API](https://dictionaryapi.dev/)
- [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations)
- [Android offline-first guidance](https://developer.android.com/topic/architecture/data-layer/offline-first)
- [Type-safe Navigation Compose routes](https://developer.android.com/guide/navigation/design/type-safety)
