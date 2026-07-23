package com.cailiangzhe.lexidue.domain.usecase

import kotlin.random.Random

fun interface RandomProvider {
    fun nextInt(untilExclusive: Int): Int
}

/** Stateful seeded randomness for repeatable question generation and tests. */
class SeededRandomProvider(
    seed: Long,
) : RandomProvider {
    private val random = Random(seed)

    override fun nextInt(untilExclusive: Int): Int = random.nextInt(untilExclusive)
}
