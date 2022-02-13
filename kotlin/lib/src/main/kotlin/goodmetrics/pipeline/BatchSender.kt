package goodmetrics.pipeline

import goodmetrics.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BatchSender private constructor() {
    companion object {
        fun <TBatch> CoroutineScope.launchSender(upstream: MetricsPipeline<TBatch>, client: Client, send: suspend Client.(batch: TBatch) -> Unit): Job {
            return launch {
                upstream.consume()
                    .collect { batch ->
                        client.send(batch)
                    }
            }
        }
    }
}
