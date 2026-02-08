package com.nekgamebling.infrastructure.external.stub

import application.port.outbound.PlayerAdapter

class StubPlayerAdapter : PlayerAdapter {
    override suspend fun findCurrentBetLimit(playerId: String): Result<Long?> {
        return Result.success(null)
    }
}