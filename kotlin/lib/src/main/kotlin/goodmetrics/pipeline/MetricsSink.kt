package goodmetrics.pipeline

import goodmetrics.Metrics

interface MetricsSink {
    fun emit(metrics: Metrics)
}
