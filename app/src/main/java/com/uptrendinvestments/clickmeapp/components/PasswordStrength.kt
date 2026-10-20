package com.uptrendinvestments.clickmeapp.components

/** Password strength bucket, ported unchanged from iOS `PasswordStrength.evaluate`. Pure Kotlin so signup can reuse it. */
enum class PasswordStrength {
    Empty,
    Weak,
    Fair,
    Strong,
    ;

    companion object {
        private const val LONG_LENGTH = 8
        private const val LONGER_LENGTH = 12
        private const val MAX_WEAK_SCORE = 1
        private const val MAX_FAIR_SCORE = 3
        private val UPPER = Regex("[A-Z]")
        private val DIGIT = Regex("[0-9]")
        private val SYMBOL = Regex("[^A-Za-z0-9]")

        fun evaluate(password: String): PasswordStrength {
            if (password.isEmpty()) return Empty
            val score =
                listOf(
                    password.length >= LONG_LENGTH,
                    password.length >= LONGER_LENGTH,
                    UPPER.containsMatchIn(password),
                    DIGIT.containsMatchIn(password),
                    SYMBOL.containsMatchIn(password),
                ).count { it }
            return when {
                score <= MAX_WEAK_SCORE -> Weak
                score <= MAX_FAIR_SCORE -> Fair
                else -> Strong
            }
        }
    }
}
