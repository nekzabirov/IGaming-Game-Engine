package application.event

import domain.model.Spin

data class SpinEvent(val spin: Spin) : ApplicationEvent
