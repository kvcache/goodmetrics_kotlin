package goodmetrics

import goodmetrics.pipeline.BatchSender.Companion.launchSender
import goodmetrics.pipeline.Batcher
import goodmetrics.pipeline.SynchronizingBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

data class ConfiguredMetrics(
    val emitterJob: Job,
    val metricsFactory: MetricsFactory,
)

class MetricsSetups private constructor() {
    companion object {
        fun CoroutineScope.normalConfig(goodmetricsHost: String = "localhost", port: Int = 9573): ConfiguredMetrics {
            val incomingBuffer = SynchronizingBuffer()
            val factory = MetricsFactory(incomingBuffer)

            val batched = Batcher(incomingBuffer)
            val emitterJob = launchSender(batched, Client.connect(goodmetricsHost, port))

            return ConfiguredMetrics(
                emitterJob,
                factory,
            )
        }
    }
}
