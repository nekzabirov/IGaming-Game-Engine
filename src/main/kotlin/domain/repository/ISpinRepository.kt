package domain.repository

import domain.model.Spin

interface ISpinRepository {

    suspend fun save(spin: Spin): Spin

    suspend fun findById(id: Long): Spin?

    suspend fun findByExternalId(externalId: String): Spin?

    /**
     * Ставка ЭТОГО раунда, оплаченная бонусными деньгами, если такая была.
     *
     * По ней выигрыш узнаёт, куда ему ложиться: раунд, профинансированный бонусом, платит на
     * бонусный счёт. Ставка и выигрыш приезжают у вендоров разными транзакциями, общее у них —
     * раунд, поэтому связь ищется по нему, а не по идентификатору транзакции.
     */
    suspend fun findBonusPlaceByRound(roundId: Long): Spin?
}
