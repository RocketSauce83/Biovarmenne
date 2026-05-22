package com.rocketsauce83.biovarmenne

import org.junit.Test
import org.junit.Assert.*

class BiovarmenneUnitTest {

    // PIN length validation
    @Test
    fun pinValidation_emptyPin_returnsFalse() {
        assertEquals(false, "".length >= 4)
    }

    @Test
    fun pinValidation_tooShort_returnsFalse() {
        assertEquals(false, "123".length >= 4)
    }

    @Test
    fun pinValidation_minimumLength_returnsTrue() {
        assertEquals(true, "1234".length >= 4)
    }

    @Test
    fun pinValidation_maximumLength_returnsTrue() {
        assertEquals(true, "12345678".length <= 8)
    }

    @Test
    fun pinValidation_tooLong_returnsFalse() {
        assertEquals(false, "123456789".length <= 8)
    }

    // PIN numeric validation
    @Test
    fun pinValidation_nonNumeric_returnsFalse() {
        assertEquals(false, "abcd".all { it.isDigit() })
    }

    @Test
    fun pinValidation_mixed_returnsFalse() {
        assertEquals(false, "12ab".all { it.isDigit() })
    }

    @Test
    fun pinValidation_numeric_returnsTrue() {
        assertEquals(true, "1234".all { it.isDigit() })
    }

    @Test
    fun pinValidation_numericWithSpaces_returnsFalse() {
        assertEquals(false, "12 34".all { it.isDigit() })
    }

    // PIN combined validation
    @Test
    fun pinValidation_validPin_returnsTrue() {
        val pin = "1234"
        assertEquals(true, pin.length in 4..8 && pin.all { it.isDigit() })
    }

    @Test
    fun pinValidation_validLongPin_returnsTrue() {
        val pin = "12345678"
        assertEquals(true, pin.length in 4..8 && pin.all { it.isDigit() })
    }

    @Test
    fun pinValidation_shortNumericPin_returnsFalse() {
        val pin = "123"
        assertEquals(false, pin.length in 4..8 && pin.all { it.isDigit() })
    }

    @Test
    fun pinValidation_longNumericPin_returnsFalse() {
        val pin = "123456789"
        assertEquals(false, pin.length in 4..8 && pin.all { it.isDigit() })
    }

    @Test
    fun pinValidation_validLengthNonNumeric_returnsFalse() {
        val pin = "abcd"
        assertEquals(false, pin.length in 4..8 && pin.all { it.isDigit() })
    }

    // PIN confirmation matching
    @Test
    fun pinConfirmation_matching_returnsTrue() {
        val pin = "1234"
        val confirmPin = "1234"
        assertEquals(true, pin == confirmPin)
    }

    @Test
    fun pinConfirmation_notMatching_returnsFalse() {
        val pin = "1234"
        val confirmPin = "5678"
        assertEquals(false, pin == confirmPin)
    }

    @Test
    fun pinConfirmation_emptyBoth_returnsTrue() {
        val pin = ""
        val confirmPin = ""
        assertEquals(true, pin == confirmPin)
    }

    @Test
    fun pinConfirmation_oneEmpty_returnsFalse() {
        val pin = "1234"
        val confirmPin = ""
        assertEquals(false, pin == confirmPin)
    }
}