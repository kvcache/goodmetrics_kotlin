package goodmetrics.pipeline

import kotlin.test.Test
import kotlin.test.assertEquals

class AggregatorKtTest {
    private fun assertBucket(expected: Long, value: Long) {
        assertEquals(expected, bucket(value), "bucket( $value )")
    }
    private fun assertBucketBelow(expected: Long, value: Long) {
        assertEquals(expected, bucketBelow(value), "bucketBelow( $value )")
    }

    private fun assertBucketBase2(expected: Long, value: Long) {
        assertEquals(expected, bucketBase2(value), "bucket( $value )")
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
        assertBucket(120, 120)
        assertBucket(130, 121)
        assertBucket(130, 123)
        assertBucket(130, 129)
        assertBucket(130, 130)
        assertBucket(120, 111)
        assertBucket(8900, 8891)
        assertBucket(8900, 8891)
    }

    @Test
    fun testBucketBelow() {
        assertBucketBelow(0, 0)
        assertBucketBelow(0, 1)
        assertBucketBelow(1, 2)
        assertBucketBelow(8, 9)
        assertBucketBelow(9, 10)
        assertBucketBelow(10, 11)
        assertBucketBelow(98, 99)
        assertBucketBelow(99, 100)
        assertBucketBelow(100, 101)
        assertBucketBelow(100, 109)
        assertBucketBelow(100, 110)
        assertBucketBelow(110, 111)
        assertBucketBelow(110, 120)
        assertBucketBelow(120, 121)
        assertBucketBelow(120, 123)
        assertBucketBelow(120, 129)
        assertBucketBelow(120, 130)
        assertBucketBelow(130, 131)
        assertBucketBelow(8800, 8891)
        assertBucketBelow(8800, 8891)
        assertBucketBelow(8900, 8901)
    }

    @Test
    fun testBucketBase2() {
        assertBucketBase2(0, 0)
        assertBucketBase2(1, 1)
        assertBucketBase2(2, 2)
        assertBucketBase2(4, 3)
        assertBucketBase2(4, 4)
        assertBucketBase2(8, 5)
        assertBucketBase2(16, 9)
        assertBucketBase2(16, 10)
        assertBucketBase2(16, 11)
        assertBucketBase2(128, 99)
        assertBucketBase2(128, 100)
        assertBucketBase2(128, 101)
        assertBucketBase2(128, 109)
        assertBucketBase2(128, 110)
        assertBucketBase2(128, 120)
        assertBucketBase2(128, 121)
        assertBucketBase2(128, 123)
        assertBucketBase2(128, 128)
        assertBucketBase2(256, 129)
        assertBucketBase2(256, 130)
        assertBucketBase2(128, 111)
        assertBucketBase2(16384, 8891)
        assertBucketBase2(16384, 8901)

        assertBucketBase2(0, -0)
        assertBucketBase2(0, -1)
        assertBucketBase2(0, -2)
        assertBucketBase2(0, -3)
        assertBucketBase2(0, -4)
        assertBucketBase2(0, -5)
        assertBucketBase2(0, -9)
        assertBucketBase2(0, -10)
        assertBucketBase2(0, -11)
        assertBucketBase2(0, -99)
        assertBucketBase2(0, -100)
        assertBucketBase2(0, -101)
        assertBucketBase2(0, -109)
        assertBucketBase2(0, -110)
        assertBucketBase2(0, -120)
        assertBucketBase2(0, -121)
        assertBucketBase2(0, -123)
        assertBucketBase2(0, -128)
        assertBucketBase2(0, -129)
        assertBucketBase2(0, -130)
        assertBucketBase2(0, -111)
        assertBucketBase2(0, -8891)
        assertBucketBase2(0, -8901)
    }
}
