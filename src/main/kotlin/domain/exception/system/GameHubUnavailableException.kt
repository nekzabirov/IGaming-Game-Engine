package domain.exception.system

class GameHubUnavailableException(message: String? = null) : SystemException(message ?: "GameHub is unavailable")
