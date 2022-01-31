package goodmetrics.pipeline

import goodmetrics.Metrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Threadsafe pipeline starter
 */
class SynchronizingBuffer(
    maxQueuedItems: Int = 1024
) : MetricsPipeline<Metrics> {
    private val metricsQueue: Channel<Metrics> = Channel(
        capacity = maxQueuedItems,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        onUndeliveredElement = this::failedToDeliver
    )

    override fun consume(): Flow<Metrics> {
        return metricsQueue.consumeAsFlow()
    }

    fun emit(metrics: Metrics) {
        metricsQueue.trySend(metrics)
    }

    private fun failedToDeliver(metrics: Metrics) {
        // TODO: record metrics
    }
}
