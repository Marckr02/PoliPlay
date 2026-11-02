package com.example.campuspocket.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CampusFormatsPercentTest {

    @Test fun `porcentaje ya multiplicado por 100 no se vuelve a multiplicar`() {
        assertEquals("4,3", CampusFormats.percentValue(java.math.BigDecimal("4.3")))
        assertEquals("130,0", CampusFormats.percentValue(java.math.BigDecimal("130")))
        assertEquals("0,0", CampusFormats.percentValue(java.math.BigDecimal.ZERO))
    }
}
