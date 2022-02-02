package goodmetrics.pipeline

import goodmetrics.Client
import goodmetrics.Metrics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class BatchSender private constructor() {
    companion object {
        fun CoroutineScope.launchSender(upstream: MetricsPipeline<List<Metrics>>, client: Client): Job {
            return launch {
                upstream.consume()
                    .collect { batch ->
                        client.sendMetrics(batch)
                    }
            }
        }
    }
}
