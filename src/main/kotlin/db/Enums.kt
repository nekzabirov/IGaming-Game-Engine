package db

import kotlinx.serialization.Serializable

@Serializable
enum class Platform {
    DESKTOP,
    MOBILE,
    DOWNLOAD,
}

/** Stored by name in `spins.type` and published by name on the event wire. */
@Serializable
enum class SpinType {
    PLACE,
    SETTLE,
    ROLLBACK,
}
