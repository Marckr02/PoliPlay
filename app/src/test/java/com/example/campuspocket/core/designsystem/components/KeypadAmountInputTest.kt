package com.example.campuspocket.core.designsystem.components

import org.junit.Assert.assertEquals
import org.junit.Test

class KeypadAmountInputTest {

    @Test
    fun `cashier input appends digits as cents`() {
        var digits = ""
        listOf("1", "2", "5", "0").forEach { key ->
            digits = keypadNextDigits(digits, key)
        }
        assertEquals("1250", digits)
    }

    @Test
    fun `formatted amount is reduced to digits before appending`() {
        assertEquals("12500", keypadNextDigits("12,50", "0"))
    }

    @Test
    fun `backspace removes the last digit`() {
        assertEquals("125", keypadNextDigits("1250", KEYPAD_BACKSPACE))
    }

    @Test
    fun `leading zeros are ignored`() {
        assertEquals("1", keypadNextDigits("0001", ""))
        assertEquals("", keypadNextDigits("0", "0"))
    }

    @Test
    fun `double zero key appends two zeros`() {
        assertEquals("500", keypadNextDigits("5", "00"))
    }

    @Test
    fun `amount is capped at nine digits`() {
        assertEquals("123456789", keypadNextDigits("123456789", "0"))
        assertEquals("999999990", keypadNextDigits("99999999", "00"))
    }
}
