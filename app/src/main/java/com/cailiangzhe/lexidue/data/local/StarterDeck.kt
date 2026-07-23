package com.cailiangzhe.lexidue.data.local

import com.cailiangzhe.lexidue.domain.model.PartOfSpeech

data class StarterDeckEntry(
    val spelling: String,
    val partOfSpeech: PartOfSpeech,
    val definition: String,
)

/**
 * Reviewed answer-key content for the local starter deck. Definitions are original plain-language
 * descriptions written for LexiDue; API responses are never imported into this list.
 */
object StarterDeck {
    const val DECK_ID = "academic-starter-v1"
    const val SOURCE_NAME = "LexiDue original starter deck"
    const val PROVENANCE =
        "Original LexiDue wording; academic-word selection informed by the Academic Word List"

    val entries: List<StarterDeckEntry> =
        listOf(
            StarterDeckEntry("analyse", PartOfSpeech.VERB, "Examine something carefully to understand its parts or causes."),
            StarterDeckEntry("assess", PartOfSpeech.VERB, "Judge the quality, importance, or amount of something."),
            StarterDeckEntry("establish", PartOfSpeech.VERB, "Show that something is true or create it on a firm basis."),
            StarterDeckEntry("indicate", PartOfSpeech.VERB, "Point out or provide a sign of something."),
            StarterDeckEntry("interpret", PartOfSpeech.VERB, "Explain the meaning of information or an event."),
            StarterDeckEntry("maintain", PartOfSpeech.VERB, "Keep something in a particular state or continue to support a claim."),
            StarterDeckEntry("require", PartOfSpeech.VERB, "Need something because it is necessary for a purpose."),
            StarterDeckEntry("respond", PartOfSpeech.VERB, "React to a question, situation, or action."),
            StarterDeckEntry("approach", PartOfSpeech.NOUN, "A way of dealing with a task, question, or problem."),
            StarterDeckEntry("concept", PartOfSpeech.NOUN, "A general idea used to understand or organise information."),
            StarterDeckEntry("context", PartOfSpeech.NOUN, "The surrounding situation that helps explain meaning."),
            StarterDeckEntry("evidence", PartOfSpeech.NOUN, "Information that supports or challenges a conclusion."),
            StarterDeckEntry("factor", PartOfSpeech.NOUN, "One condition or influence that contributes to a result."),
            StarterDeckEntry("method", PartOfSpeech.NOUN, "An organised way of carrying out a task or investigation."),
            StarterDeckEntry("policy", PartOfSpeech.NOUN, "An agreed set of principles that guides decisions and actions."),
            StarterDeckEntry("source", PartOfSpeech.NOUN, "The place, person, or material from which information comes."),
            StarterDeckEntry("available", PartOfSpeech.ADJECTIVE, "Ready to be used, obtained, or considered."),
            StarterDeckEntry("consistent", PartOfSpeech.ADJECTIVE, "Staying in agreement or following the same pattern."),
            StarterDeckEntry("significant", PartOfSpeech.ADJECTIVE, "Important enough to deserve attention or affect a result."),
            StarterDeckEntry("specific", PartOfSpeech.ADJECTIVE, "Clearly identified and limited rather than general."),
            StarterDeckEntry("relevant", PartOfSpeech.ADJECTIVE, "Directly connected to the question or purpose."),
            StarterDeckEntry("similar", PartOfSpeech.ADJECTIVE, "Sharing important features without being exactly the same."),
            StarterDeckEntry("sufficient", PartOfSpeech.ADJECTIVE, "Enough to meet a particular need or standard."),
            StarterDeckEntry("valid", PartOfSpeech.ADJECTIVE, "Well founded and suitable for the conclusion being drawn."),
        )
}
