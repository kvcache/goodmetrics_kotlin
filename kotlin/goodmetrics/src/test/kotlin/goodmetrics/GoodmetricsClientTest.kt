package goodmetrics

import goodmetrics.downstream.GoodmetricsClient
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test

class GoodmetricsClientTest {
    @Ignore
    @Test fun someLibraryMethodReturnsTrue() {
        runBlocking {
            val goodmetricsClient = GoodmetricsClient.connect("localhost")
            goodmetricsClient.sendToyMetrics()
        }
    }
}
