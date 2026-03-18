package application.port.factory

import application.port.external.IFreespinPort
import application.port.external.IGamePort
import domain.model.Aggregator

interface IAggregatoryFactory {

    fun createGameAdapter(aggregator: Aggregator): IGamePort

    fun createFreespinAdapter(aggregator: Aggregator): IFreespinPort

}