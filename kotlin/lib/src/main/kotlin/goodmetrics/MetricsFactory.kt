@file:OptIn(ExperimentalContracts::class)

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

class MetricsConfiguration private constructor() {
    companion object {
        var epochMillis: () -> Long = System::currentTimeMillis
    }
}

class MetricsFactory(
    private val sink: MetricsSink
) {
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
            TimestampAt.Start -> MetricsConfiguration.epochMillis()
            TimestampAt.End -> -1
        }
        return Metrics(name, timestamp)
    }

    @PublishedApi internal fun emit(metrics: Metrics) {
        println(metrics)
    }
}
