package br.com.contasobcontrole.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceMathTest {
    @Test fun percentageUsesDecimalArithmetic() {
        assertEquals("25.0", FinanceMath.percent(2_500, 10_000).toPlainString())
    }
    @Test fun remainingCanBeNegative() {
        assertEquals(-500L, FinanceMath.remaining(5_000, 5_500))
    }
    @Test fun recommendedSavingRoundsHalfUp() {
        assertEquals(1_000L, FinanceMath.recommendedSaving(10_000, 10))
    }
}
