package goodmetrics.pipeline

import kotlinx.coroutines.flow.Flow

interface MetricsPipeline<T> {
    fun consume(): Flow<T>
}
