package infrastructure.unit

import application.port.external.IBackgroundTaskPort
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

class BackgroundWorker : IBackgroundTaskPort, CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Dispatchers.IO + CoroutineName("background")

    override suspend fun launch(action: suspend () -> Unit, error: suspend () -> Unit) {
        launch {
            try {
                action()
            } catch (_: Throwable) {
                error()
            }
        }
    }
}