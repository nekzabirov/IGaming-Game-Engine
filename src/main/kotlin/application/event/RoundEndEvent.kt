package application.event

import domain.model.Round

data class RoundEndEvent(val round: Round) : ApplicationEvent
