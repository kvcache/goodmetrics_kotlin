package goodmetrics.pipeline

import goodmetrics.Client
import goodmetrics.Metrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

data class ConnectionConfiguration(
    val hostname: String,
    val port: Int
)

class BatchSender private constructor(
    private val upstream: MetricsPipeline<List<Metrics>>,
    private val client: Client,
) {
    companion object {
        fun CoroutineScope.consumeAndSend() {
        }
    }
    private lateinit var consumeJob: Job

    private fun CoroutineScope.consume() {
        consumeJob = launch {
            upstream.consume()
                .collect {
                }
        }
    }
}