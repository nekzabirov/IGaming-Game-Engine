package infrastructure.util

import application.port.external.ICurrencyPort
import domain.vo.Currency

class CurrencyAdapter : ICurrencyPort {
    override suspend fun convertToUnits(amount: Double, currency: Currency): Long = (amount * 100L).toLong()

    override suspend fun convertFromUnits(amount: Long, currency: Currency): Double = (amount / 100f).toDouble()
}