package goodmetrics.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals

class AggregatorKtTest {
    private fun assertBucket(expected: Long, value: Long) {
        assertEquals(expected, bucket(value), "bucket( $value )")
    }

    @Test
    fun testBucket() {
        assertBucket(0, 0)
        assertBucket(1, 1)
        assertBucket(2, 2)
        assertBucket(9, 9)
        assertBucket(10, 10)
        assertBucket(11, 11)
        assertBucket(99, 99)
        assertBucket(100, 100)
        assertBucket(110, 101)
        assertBucket(110, 109)
        assertBucket(110, 110)
        assertBucket(120, 111)
        assertBucket(8900, 8891)
        assertBucket(8900, 8891)
    }
}
