@file:OptIn(ExperimentalContracts::class, ExperimentalContracts::class)

package goodmetrics

import goodmetrics.pipeline.MetricsSink
import java.time.Instant
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

enum class TimestampAt {
    // Stamp the metric at the start
    Start,
    // Stamp the metric at the end
    End,
}

fun interface NanoTimeSource {
    fun epochNanos(): Long

    companion object {
        val preciseNanoTime = NanoTimeSource {
            val now = Instant.now()
            now.epochSecond + now.nano
        }

        val fastNanoTime = NanoTimeSource {
            System.currentTimeMillis() * 1000000
        }
    }
}

class MetricsFactory(
    private val sink: MetricsSink,
    @PublishedApi internal val timeSource: NanoTimeSource,
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
            TimestampAt.Start -> timeSource.epochNanos()
            TimestampAt.End -> -1
        }
        return Metrics(name, timestamp, System.nanoTime())
    }

    @PublishedApi internal fun emit(metrics: Metrics) {
        finalizeMetrics(metrics)
        sink.emit(metrics)
    }

    private fun finalizeMetrics(metrics: Metrics) {
        if (metrics.timestampNanos < 1) {
            metrics.timestampNanos = timeSource.epochNanos()
        }
        val duration = System.nanoTime() - metrics.startNanoTime
        metrics.measure("totaltime", duration)
    }
}
