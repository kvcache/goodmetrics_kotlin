package goodmetrics

import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test

class ClientTest {
    @Ignore
    @Test fun someLibraryMethodReturnsTrue() {
        runBlocking {
            val client = Client.connect("localhost")
            client.sendToyMetrics()
        }
    }
}
