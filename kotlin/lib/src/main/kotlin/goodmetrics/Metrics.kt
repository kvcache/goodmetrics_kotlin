package goodmetrics

sealed interface SupportedDimensionType
data class StringDimension(val value: String) : SupportedDimensionType {
    override fun toString(): String {
        return value
    }
}
data class NumberDimension(val value: Long) : SupportedDimensionType {
    override fun toString(): String {
        return value.toString()
    }
}
data class BooleanDimension(val value: Boolean) : SupportedDimensionType {
    override fun toString(): String {
        return value.toString()
    }
}

/**
 * Not thread safe.
 */
class Metrics internal constructor(
    internal val name: String,
    internal var timestampMillis: Long,
    internal val startNanoTime: Long,
) {
    internal val metricMeasurements: MutableMap<String, Number> = mutableMapOf()
    internal val metricDistributions: MutableMap<String, Long> = mutableMapOf()
    internal val metricDimensions: MutableMap<String, SupportedDimensionType> = mutableMapOf()

    fun dimension(dimension: String, value: Boolean) {
        metricDimensions[dimension] = BooleanDimension(value)
    }

    fun dimension(dimension: String, value: Long) {
        metricDimensions[dimension] = NumberDimension(value)
    }

    fun dimension(dimension: String, value: String) {
        metricDimensions[dimension] = StringDimension(value)
    }

    fun measure(name: String, value: Long) {
        metricMeasurements[name] = value
    }

    fun measure(name: String, value: Int) {
        metricMeasurements[name] = value.toLong()
    }

    fun measure(name: String, value: Double) {
        metricMeasurements[name] = value
    }

    fun measure(name: String, value: Float) {
        metricMeasurements[name] = value.toDouble()
    }

    fun distribution(name: String, value: Long) {
        metricDistributions[name] = value
    }

    fun distribution(name: String, value: Int) {
        metricDistributions[name] = value.toLong()
    }
}
