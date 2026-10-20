package com.uptrendinvestments.clickmeapp.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordStrengthTest {
    @Test
    fun empty_isEmpty() {
        assertEquals(PasswordStrength.Empty, PasswordStrength.evaluate(""))
    }

    @Test
    fun scoreZeroAndOne_areWeak() {
        assertEquals(PasswordStrength.Weak, PasswordStrength.evaluate("abc"))
        assertEquals(PasswordStrength.Weak, PasswordStrength.evaluate("abcdefgh"))
    }

    @Test
    fun scoreTwoAndThree_areFair() {
        assertEquals(PasswordStrength.Fair, PasswordStrength.evaluate("abcdefgh1"))
        assertEquals(PasswordStrength.Fair, PasswordStrength.evaluate("Abcdefgh1"))
    }

    @Test
    fun scoreFourAndFive_areStrong() {
        assertEquals(PasswordStrength.Strong, PasswordStrength.evaluate("Abcdefgh1!"))
        assertEquals(PasswordStrength.Strong, PasswordStrength.evaluate("Abcdefghij1!"))
    }

    @Test
    fun onlyUppercase_isWeak() {
        assertEquals(PasswordStrength.Weak, PasswordStrength.evaluate("ABC"))
    }

    @Test
    fun longSymbolOnly_isFair() {
        assertEquals(PasswordStrength.Fair, PasswordStrength.evaluate("!!!!!!!!!!!!"))
    }
}
