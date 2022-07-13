package goodmetrics.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals

class AggregatorKtTest {

    @Test
    fun testZero() {
        assertEquals(0, bucket(0))
    }

    @Test
    fun testOne() {
        assertEquals(1, bucket(1))
    }

    @Test
    fun testOneHundred() {
        assertEquals(100, bucket(100))
    }

    @Test
    fun test101() {
        assertEquals(110, bucket(101))
    }

    @Test
    fun testBucket() {
        assertEquals(2, bucket(2))
        assertEquals(9, bucket(9))
        assertEquals(10, bucket(10))
        assertEquals(11, bucket(11))
        assertEquals(99, bucket(99))
        assertEquals(110, bucket(109))
        assertEquals(110, bucket(110))
        assertEquals(120, bucket(111))
        assertEquals(8900, bucket(8891))
        assertEquals(8900, bucket(8891))
    }
}