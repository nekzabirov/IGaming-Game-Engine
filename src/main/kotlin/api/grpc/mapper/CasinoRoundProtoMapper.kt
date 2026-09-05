package api.grpc.mapper

import api.grpc.mapper.CasinoGameProtoMapper.toProto
import application.query.round.CasinoRoundView
import com.nekgamebling.game.v1.CasinoRoundDto
import com.nekgamebling.game.v1.CasinoRoundViewDto
import com.nekgamebling.game.v1.casinoRoundDto
import com.nekgamebling.game.v1.casinoRoundViewDto
import domain.model.CasinoRound

object CasinoRoundProtoMapper {

    /**
     * `game` is left unset for a sportsbook leg — the hub sends an empty game for a bet and the
     * round is stored without one. It is deliberately not replaced by a placeholder: a reader has
     * to be able to tell a bet from a spin, and `has_game` is that seam.
     */
    fun CasinoRound.toProto(): CasinoRoundDto = casinoRoundDto {
        id = this@toProto.id
        externalId = this@toProto.externalId.value
        this@toProto.freespinId?.let { freespinId = it.value }
        playerId = this@toProto.playerId.value
        this@toProto.game?.let { game = it.toProto() }
        currency = this@toProto.currency.value
        createdAt = this@toProto.createdAt.toString()
        this@toProto.finishedAt?.let { finishedAt = it.toString() }
    }

    fun CasinoRoundView.toProto(): CasinoRoundViewDto = casinoRoundViewDto {
        round = this@toProto.round.toProto()
        totalPlace = this@toProto.totalPlace.value
        totalSettle = this@toProto.totalSettle.value
    }
}
