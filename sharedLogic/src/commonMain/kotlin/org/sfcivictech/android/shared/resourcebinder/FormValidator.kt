package org.sfcivictech.android.shared.resourcebinder

object FormValidator {
    private const val MIN_TEXT_LENGTH = 2

    // Deliberately loose: one @, no spaces, and a dot-separated domain. The server is
    // the real authority on whether an address can receive mail.
    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")

    fun isValidFullName(fullName: String): Boolean = isTextWithLetters(fullName)

    fun isValidEmail(email: String): Boolean = emailPattern.matches(email.trim())

    fun isValidOrganization(organization: String): Boolean = isTextWithLetters(organization)

    // Length alone would let "1" through, and digits alone would let "1234567" through
    private fun isTextWithLetters(value: String): Boolean {
        val trimmed = value.trim()
        return trimmed.length >= MIN_TEXT_LENGTH && trimmed.any { it.isLetter() }
    }
}
