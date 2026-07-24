# Dictionary enrichment source decision

**Reviewed:** 23 July 2026  
**Status:** Approved for the assessed coursework build with the safeguards below; release outside coursework requires a fresh rights review.

## Source and intended use

LexiDue uses the public HTTPS v2 endpoint documented by the [Free Dictionary API](https://dictionaryapi.dev/):

```text
GET https://api.dictionaryapi.dev/api/v2/entries/en/{word}
```

The provider describes the service as free and explicitly identifies learning applications as an intended use. It does not require a key or user account. The service is nevertheless an external, best-effort dependency with no availability promise, so it is never required for practice.

## Rights and licence finding

The provider's [server repository](https://github.com/meetDeveloper/freeDictionaryAPI) is published under GPL-3.0. That licence clearly covers the repository software; it does not by itself establish that every dictionary definition, example, phonetic string, or third-party audio resource returned by the service may be redistributed. The provider pages reviewed above do not publish separate response-data, caching, or audio terms.

Because those content rights are not explicit, this project applies a narrow coursework policy:

- enrichment is optional and starts only after the learner taps the Home action;
- each action sends at most five due English word spellings and never sends answers, scores, progress, settings, or a user identifier;
- validated text is cached only on the learner's device for offline display, becomes stale after 30 days, and is not bundled or exported;
- the UI identifies **Free Dictionary API** and displays the source URL stored with the record;
- API senses remain separate from the reviewed starter-deck meanings and cannot become quiz answers or distractors;
- audio URLs may be validated and stored as metadata, but this build does not download, bundle, or play third-party audio;
- automated tests use original synthetic JSON fixtures rather than copied live responses.

Before a public or commercial release, the maintainer must obtain explicit response-content and cache permission from the provider, document any underlying dictionary/audio licences, or replace this source with one whose reuse terms are unambiguous.

## Failure and privacy behaviour

The request path necessarily reveals the selected word and ordinary connection metadata (for example, IP address and user agent) to the provider and its infrastructure. Home explains this before the action. No background or bulk refresh is performed.

Each complete request has a 15-second call timeout, and responses larger than 512 KiB are rejected before deserialisation. Timeouts, network failures, non-success HTTP responses, malformed or oversized payloads, mismatched words, and entries without any usable definition are treated as failed enrichment. An unsafe optional audio URL is omitted rather than failing an otherwise usable sense. The lookup fails only if validation leaves no usable sense. A failure never deletes saved enrichment and never changes the canonical local deck, review schedule, or practice availability.
