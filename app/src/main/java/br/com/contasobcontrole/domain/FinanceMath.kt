package br.com.contasobcontrole.domain

import java.math.BigDecimal
import java.math.RoundingMode

object FinanceMath {
    fun percent(part: Long, total: Long): BigDecimal =
        if (total <= 0) BigDecimal.ZERO else BigDecimal.valueOf(part)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)

    fun remaining(income: Long, expenses: Long): Long = income - expenses
    fun recommendedSaving(income: Long, rate: Int = 10): Long =
        BigDecimal.valueOf(income).multiply(BigDecimal.valueOf(rate.toLong()))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).toLong()
}
