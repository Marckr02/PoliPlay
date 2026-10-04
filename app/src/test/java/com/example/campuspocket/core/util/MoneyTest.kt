package com.example.campuspocket.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MoneyTest {

    @Test fun `punto decimal`() {
        assertEquals(1999L, Money.parseToCents("19.99"))
        assertEquals(10L, Money.parseToCents("0.10"))
        assertEquals(7L, Money.parseToCents("0.07"))
    }

    @Test fun `el resultado no depende de decimales binarios`() {
        // Con Double, 19.99 * 100 daría 1998.9999999999998 y se truncaría a 1998.
        assertEquals(1999L, Money.parseToCents("19.99"))
        assertEquals(2999L, Money.parseToCents("29.99"))
        assertEquals(110L, Money.parseToCents("1.10"))
    }

    @Test fun `numeros negativos`() {
        assertEquals(-525L, Money.parseToCents("-5.25"))
        assertEquals(-500L, Money.parseToCents("-5"))
        assertEquals(525L, Money.parseToCents("+5.25"))
        assertEquals(0L, Money.parseToCents("-0"))
    }

    @Test fun `coma decimal`() {
        assertEquals(150L, Money.parseToCents("1,5"))
        assertEquals(1999L, Money.parseToCents("19,99"))
    }

    @Test fun `separadores de miles`() {
        assertEquals(123456L, Money.parseToCents("1.234,56"))
        assertEquals(123456L, Money.parseToCents("1,234.56"))
        assertEquals(123456700L, Money.parseToCents("1.234.567"))
        assertEquals(123456700L, Money.parseToCents("1,234,567"))
    }

    @Test fun `enteros y espacios`() {
        assertEquals(500L, Money.parseToCents("5"))
        assertEquals(500L, Money.parseToCents("  5  "))
        assertEquals(1050L, Money.parseToCents("$10.50"))
    }

    @Test fun `sin parte entera`() {
        assertEquals(50L, Money.parseToCents(".5"))
        assertEquals(5L, Money.parseToCents(",05"))
    }

    @Test fun `entradas invalidas devuelven null`() {
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("   "))
        assertNull(Money.parseToCents("-"))
        assertNull(Money.parseToCents("."))
        assertNull(Money.parseToCents("1.2.3,4.5"))
        assertNull(Money.parseToCents("12abc"))
        assertNull(Money.parseToCents("--5"))
    }

    @Test fun `mas de dos decimales se rechaza`() {
        assertNull(Money.parseToCents("1.234"))
        assertNull(Money.parseToCents("0.005"))
        assertNull(Money.parseToCents("1,999"))
    }

    @Test fun `numeros demasiado grandes devuelven null`() {
        assertNull(Money.parseToCents("99999999999999999999"))
    }

    @Test fun `texto plano editable`() {
        assertEquals("19.99", Money.toPlainString(1999))
        assertEquals("0.07", Money.toPlainString(7))
        assertEquals("-5.25", Money.toPlainString(-525))
        assertEquals("0.00", Money.toPlainString(0))
    }

    @Test fun `ida y vuelta no pierde centavos`() {
        for (cents in listOf(0L, 1L, 7L, 99L, 100L, 1999L, 123456L, -1L, -525L)) {
            assertEquals(cents, Money.parseToCents(Money.toPlainString(cents)))
        }
    }

    @Test fun `formato de moneda con locale explicito`() {
        assertEquals("$1,234.56", Money.format(123456, "USD", Locale.US))
        assertEquals("-$5.25", Money.format(-525, "USD", Locale.US))
        assertEquals("$0.07", Money.format(7, "USD", Locale.US))
        assertEquals("+$19.99", Money.formatSigned(1999, "USD", Locale.US))
    }
}
