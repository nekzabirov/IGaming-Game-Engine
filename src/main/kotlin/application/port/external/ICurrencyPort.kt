package application.port.external

import domain.vo.Currency
import java.math.BigDecimal

/**
 * Port interface for currency operations.
 */
interface ICurrencyPort {
    suspend fun convertToUnits(amount: Double, currency: Currency): Long

    suspend fun convertFromUnits(amount: Long, currency: Currency): Double
}
