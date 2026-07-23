package com.cailiangzhe.lexidue.data.local

import androidx.room.TypeConverter
import com.cailiangzhe.lexidue.domain.model.AttemptOutcome
import com.cailiangzhe.lexidue.domain.model.PartOfSpeech
import com.cailiangzhe.lexidue.domain.model.PracticeDifficulty
import com.cailiangzhe.lexidue.domain.model.QuestionType
import com.cailiangzhe.lexidue.domain.model.SessionStatus
import kotlinx.serialization.json.Json

class AppTypeConverters {
    @TypeConverter
    fun stringListToJson(value: List<String>): String = Json.encodeToString(value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun partOfSpeechToString(value: PartOfSpeech): String = value.name

    @TypeConverter
    fun stringToPartOfSpeech(value: String): PartOfSpeech = PartOfSpeech.valueOf(value)

    @TypeConverter
    fun difficultyToString(value: PracticeDifficulty): String = value.name

    @TypeConverter
    fun stringToDifficulty(value: String): PracticeDifficulty = PracticeDifficulty.valueOf(value)

    @TypeConverter
    fun sessionStatusToString(value: SessionStatus): String = value.name

    @TypeConverter
    fun stringToSessionStatus(value: String): SessionStatus = SessionStatus.valueOf(value)

    @TypeConverter
    fun questionTypeToString(value: QuestionType): String = value.name

    @TypeConverter
    fun stringToQuestionType(value: String): QuestionType = QuestionType.valueOf(value)

    @TypeConverter
    fun attemptOutcomeToString(value: AttemptOutcome): String = value.name

    @TypeConverter
    fun stringToAttemptOutcome(value: String): AttemptOutcome = AttemptOutcome.valueOf(value)
}
