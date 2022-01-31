package goodmetrics.pipeline

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class Batcher<TUpstream>(
    private val upstream: MetricsPipeline<TUpstream>,
    private val batchSize: Int = 1000,
    private val batchAge: Duration = 10.seconds,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : MetricsPipeline<List<TUpstream>> {
    override fun consume(): Flow<List<TUpstream>> {
        var currentBatch = ArrayList<TUpstream>(batchSize)
        var currentBatchDeadline = timeSource.markNow() + batchAge
        return upstream.consume()
            .transform { item ->
                currentBatch.add(item)
                if (batchSize <= currentBatch.size || currentBatchDeadline.hasPassedNow()) {
                    emit(currentBatch)
                    currentBatchDeadline = timeSource.markNow() + batchAge
                    currentBatch = ArrayList(batchSize)
                }
            }
    }
}
