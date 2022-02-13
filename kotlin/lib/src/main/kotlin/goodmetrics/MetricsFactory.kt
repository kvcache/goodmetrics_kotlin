@file:OptIn(ExperimentalContracts::class, ExperimentalContracts::class)

package goodmetrics

import goodmetrics.pipeline.MetricsSink
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

enum class TimestampAt {
    // Stamp the metric at the start
    Start,
    // Stamp the metric at the end
    End,
}

class MetricsFactory(
    private val sink: MetricsSink,
    @PublishedApi internal val epochMillis: () -> Long = System::currentTimeMillis,
) {
    /**
     * Passes a Metrics into your scope. Record your unit of work; when the scope exits
     * the Metrics will be stamped with `totaltime` and emitted through the pipeline.
     */
    inline fun <T> record(name: String, stampAt: TimestampAt = TimestampAt.Start, block: (Metrics) -> T): T {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }
        val metrics = getMetrics(name, stampAt)
        try {
            return block(metrics)
        } finally {
            emit(metrics)
        }
    }

    @PublishedApi internal fun getMetrics(name: String, stampAt: TimestampAt): Metrics {
        val timestamp = when (stampAt) {
            TimestampAt.Start -> epochMillis()
            TimestampAt.End -> -1
        }
        return Metrics(name, timestamp, System.nanoTime())
    }

    @PublishedApi internal fun emit(metrics: Metrics) {
        finalizeMetrics(metrics)
        sink.emit(metrics)
    }

    private fun finalizeMetrics(metrics: Metrics) {
        if (metrics.timestampMillis < 1) {
            metrics.timestampMillis = epochMillis()
        }
        val duration = System.nanoTime() - metrics.startNanoTime
        metrics.measure("totaltime", duration)
    }
}
