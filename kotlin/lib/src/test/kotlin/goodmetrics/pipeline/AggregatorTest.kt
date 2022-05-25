package goodmetrics.pipeline

import goodmetrics.Metrics
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class AggregatorTest {
    lateinit var gotBatch: Mutex
    lateinit var aggregatorSleep: Mutex

    val batches = mutableListOf<AggregatedBatch>()

    fun endAggregatorSleep() {
        assertTrue(aggregatorSleep.isLocked)
        aggregatorSleep.unlock()
    }

    suspend fun sleepAggregator() {
        aggregatorSleep.lock()
    }

    @BeforeTest
    fun reset() {
        aggregatorSleep = Mutex(true)
        gotBatch = Mutex(true)
        batches.clear()
    }

    suspend fun oneSecondDelay(delay: Duration) {
        assertEquals(1.seconds, delay)
        sleepAggregator()
    }

    fun metrics(): Metrics {
        val m = Metrics("test", 123, 456)
        m.dimension("tes", "t")
        m.measure("f", 5)
        return m
    }

    @Test
    fun testConsume() {
        runBlocking {
            val aggregator = Aggregator(aggregationWidth = 1.seconds, delay_fn = ::oneSecondDelay)
            val collectorJob = launch {
                aggregator.consume().collect { batch ->
                    batches.add(batch)
                    gotBatch.unlock()
                }
            }

            aggregator.emit(metrics())
            endAggregatorSleep()
            gotBatch.lock()

            assertEquals(1, batches.size)
            val batch = batches[0]
            assertEquals("test", batch.metric)
            assertEquals(setOf(setOf<Metrics.Dimension>(Metrics.Dimension.Str("tes", "t"))), batch.positions.keys)
            val aggregations = batch.positions[setOf<Metrics.Dimension>(Metrics.Dimension.Str("tes", "t"))]!!

            assertEquals(setOf("f"), aggregations.keys)
            val aggregation = aggregations["f"]!!
            assertTrue(aggregation is Aggregation.StatisticSet)
            assertEquals(5.0, aggregation.min.get())
            assertEquals(5.0, aggregation.max.get())
            assertEquals(5.0, aggregation.sum.sum())
            assertEquals(1L, aggregation.count.sum())

            collectorJob.cancel()
        }
    }

    @Test
    fun emit() {
    }
}
