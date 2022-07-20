package goodmetrics.pipeline

import goodmetrics.downstream.GoodmetricsClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BatchSender private constructor() {
    companion object {
        fun <TBatch> CoroutineScope.launchSender(upstream: MetricsPipeline<TBatch>, client: GoodmetricsClient, send: suspend GoodmetricsClient.(batch: TBatch) -> Unit): Job {
            return launch {
                upstream.consume()
                    .collect { batch ->
                        client.send(batch)
                    }
            }
        }
    }
}
