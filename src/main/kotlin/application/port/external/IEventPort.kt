package application.port.external

import application.event.ApplicationEvent

interface IEventPort {

    suspend fun publish(event: ApplicationEvent)

}