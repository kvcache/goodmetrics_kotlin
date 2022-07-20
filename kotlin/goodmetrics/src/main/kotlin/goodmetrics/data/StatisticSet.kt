package goodmetrics.data

/**
 * Not thread safe.
 */
class StatisticSet(
    val min: Double,
    val max: Double,
    val sum: Double,
    val count: Long,
) {
    operator fun plus(other: StatisticSet) = StatisticSet(
        min = kotlin.math.min(min, other.min),
        max = kotlin.math.max(max, other.max),
        sum = sum + other.sum,
        count = count + other.count,
    )

    operator fun plus(number: Number) = StatisticSet(
        min = kotlin.math.min(min, number.toDouble()),
        max = kotlin.math.max(max, number.toDouble()),
        sum = sum + number.toDouble(),
        count = count + 1,
    )
}
