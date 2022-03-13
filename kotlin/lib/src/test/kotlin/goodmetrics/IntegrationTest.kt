package goodmetrics

import goodmetrics.MetricsSetups.Companion.rowPerMetric
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.net.Inet4Address
import kotlin.random.Random

class IntegrationTest {
    //@Ignore
    @Test
    fun testMetrics() = runBlocking {
        val metricsBackgroundScope = CoroutineScope(Dispatchers.Default)
        val (emitterJob, metricsFactory) = metricsBackgroundScope.rowPerMetric()

        for (i in 1..1000) {
            metricsFactory.record("demo_app") { metrics ->
                metrics.measure("iteration", i)
                metrics.dimension("random_boolean", Random.nextBoolean())
                metrics.measure("random_float", Random.nextFloat())
                metrics.dimension("host", Inet4Address.getLocalHost().hostName)
            }
        }
        metricsBackgroundScope.cancel()
        emitterJob.join()
    }
}
