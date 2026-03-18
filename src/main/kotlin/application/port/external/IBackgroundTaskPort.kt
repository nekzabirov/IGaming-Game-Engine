package application.port.external

interface IBackgroundTaskPort {

    suspend fun launch(action: suspend () -> Unit, error: suspend () -> Unit = {})

}