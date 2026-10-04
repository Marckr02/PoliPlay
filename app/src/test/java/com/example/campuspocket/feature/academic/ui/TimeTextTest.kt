package com.example.campuspocket.feature.academic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalTime

class TimeTextTest {

    @Test
    fun `hora valida en formato de 24 h`() {
        assertEquals(LocalTime.of(7, 0), TimeText.parse("7:00"))
        assertEquals(LocalTime.of(7, 0), TimeText.parse("07:00"))
        assertEquals(LocalTime.of(17, 30), TimeText.parse("17:30"))
        assertEquals(LocalTime.of(9, 5), TimeText.parse(" 9:05 ") )
    }

    @Test
    fun `texto invalido devuelve null`() {
        assertNull(TimeText.parse(""))
        assertNull(TimeText.parse("   "))
        assertNull(TimeText.parse("25:00"))
        assertNull(TimeText.parse("7"))
        assertNull(TimeText.parse("07:60"))
        assertNull(TimeText.parse("abc"))
    }

    @Test
    fun `al formatear sale en 24 h sin ceros a la izquierda`() {
        assertEquals("7:00", TimeText.format(LocalTime.of(7, 0)))
        assertEquals("17:45", TimeText.format(LocalTime.of(17, 45)))
    }
}
