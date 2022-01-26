package goodmetrics

import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ClientTest {
    @Test fun someLibraryMethodReturnsTrue() {
        runBlocking {
            val client = Client.connect("localhost")
            client.sendMetrics()
        }
    }
}
