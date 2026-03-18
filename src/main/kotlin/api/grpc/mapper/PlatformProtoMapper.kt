package api.grpc.mapper

import com.nekgamebling.game.v1.PlatformDto
import domain.model.Platform

object PlatformProtoMapper {

    fun Platform.toProto(): PlatformDto = when (this) {
        Platform.DESKTOP -> PlatformDto.PLATFORM_DESKTOP
        Platform.MOBILE -> PlatformDto.PLATFORM_MOBILE
        Platform.DOWNLOAD -> PlatformDto.PLATFORM_DOWNLOAD
    }

    fun PlatformDto.toDomain(): Platform = when (this) {
        PlatformDto.PLATFORM_DESKTOP -> Platform.DESKTOP
        PlatformDto.PLATFORM_MOBILE -> Platform.MOBILE
        PlatformDto.PLATFORM_DOWNLOAD -> Platform.DOWNLOAD
        PlatformDto.PLATFORM_UNSPECIFIED, PlatformDto.UNRECOGNIZED -> Platform.DESKTOP
    }
}
