package goodmetrics

import goodmetrics.downstream.GoodmetricsClient
import goodmetrics.pipeline.Aggregator
import goodmetrics.pipeline.BatchSender.Companion.launchSender
import goodmetrics.pipeline.Batcher
import goodmetrics.pipeline.SynchronizingBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ConfiguredMetrics(
    val emitterJob: Job,
    val metricsFactory: MetricsFactory,
)

class MetricsSetups private constructor() {
    companion object {
        fun CoroutineScope.rowPerMetric(goodmetricsHost: String = "localhost", port: Int = 9573): ConfiguredMetrics {
            val incomingBuffer = SynchronizingBuffer()
            val factory = MetricsFactory(incomingBuffer, timeSource = NanoTimeSource.preciseNanoTime)

            val batched = Batcher(incomingBuffer)
            val emitterJob = launchSender(batched, GoodmetricsClient.connect(goodmetricsHost, port)) { batch ->
                sendMetricsBatch(batch)
            }

            return ConfiguredMetrics(
                emitterJob,
                factory,
            )
        }

        fun CoroutineScope.preaggregated(goodmetricsHost: String = "localhost", port: Int = 9573, aggregationWidth: Duration = 10.seconds): ConfiguredMetrics {
            val incomingBuffer = Aggregator(aggregationWidth)
            val factory = MetricsFactory(incomingBuffer, timeSource = NanoTimeSource.fastNanoTime)

            val batched = Batcher(incomingBuffer)
            val emitterJob = launchSender(batched, GoodmetricsClient.connect(goodmetricsHost, port)) { batch ->
                sendPreaggregatedMetrics(batch)
            }

            return ConfiguredMetrics(
                emitterJob,
                factory,
            )
        }
    }
}
