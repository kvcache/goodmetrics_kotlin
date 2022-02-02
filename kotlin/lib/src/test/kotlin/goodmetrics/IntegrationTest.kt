package goodmetrics

import goodmetrics.MetricsSetups.Companion.normalConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.Ignore
import org.junit.Test
import java.net.Inet4Address
import kotlin.random.Random

class IntegrationTest {
    //@Ignore
    @Test
    fun testMetrics() {
        val metricsBackgroundScope = CoroutineScope(Dispatchers.Default)
        val (emitterJob, metricsFactory) = metricsBackgroundScope.normalConfig()

        for (i in 1..1000) {
            metricsFactory.record("demo_app") { metrics ->
                metrics.measureI("iteration", i)
                metrics.dimensionBool("random_boolean", Random.nextBoolean())
                metrics.measureF("random_float", Random.nextFloat())
                metrics.dimensionString("host", Inet4Address.getLocalHost().hostName)
            }
        }
        metricsBackgroundScope.cancel()
    }
}