package com.example.campuspocket.feature.academic.domain

import com.example.campuspocket.feature.academic.domain.model.SemesterTerm
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SemesterTermTest {

    @Test
    fun `enero a junio pertenece al periodo 1`() {
        val term = SemesterTerm.forDate(LocalDate.of(2026, 3, 15))
        assertEquals("2026-1", term.name)
        assertEquals(LocalDate.of(2026, 1, 1), term.start)
        assertEquals(LocalDate.of(2026, 6, 30), term.end)
    }

    @Test
    fun `julio a diciembre pertenece al periodo 2`() {
        val term = SemesterTerm.forDate(LocalDate.of(2026, 8, 20))
        assertEquals("2026-2", term.name)
        assertEquals(LocalDate.of(2026, 7, 1), term.start)
        assertEquals(LocalDate.of(2026, 12, 31), term.end)
    }

    @Test
    fun `los bordes caen en el periodo correcto`() {
        assertEquals("2026-1", SemesterTerm.forDate(LocalDate.of(2026, 6, 30)).name)
        assertEquals("2026-2", SemesterTerm.forDate(LocalDate.of(2026, 7, 1)).name)
    }
}
