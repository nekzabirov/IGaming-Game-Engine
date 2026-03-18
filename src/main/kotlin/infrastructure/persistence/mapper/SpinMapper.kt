package infrastructure.persistence.mapper

import domain.model.Spin
import domain.vo.Amount
import infrastructure.persistence.entity.SpinEntity
import infrastructure.persistence.mapper.RoundMapper.toDomain

object SpinMapper {

    fun SpinEntity.toDomain(): Spin = Spin(
        id = id.value,
        externalId = externalId,
        round = round.toDomain(),
        reference = reference?.toDomain(),
        type = type,
        amount = Amount(amount),
        realAmount = Amount(realAmount),
        bonusAmount = Amount(bonusAmount),
    )
}
