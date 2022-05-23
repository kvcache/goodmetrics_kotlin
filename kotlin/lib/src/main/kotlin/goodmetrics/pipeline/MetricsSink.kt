package goodmetrics.pipeline

import goodmetrics.Metrics

fun interface MetricsSink {
    fun emit(metrics: Metrics)
}
