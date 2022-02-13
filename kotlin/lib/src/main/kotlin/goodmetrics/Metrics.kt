package goodmetrics

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
    internal val metricDimensions: MutableMap<String, Any> = mutableMapOf()

    fun dimension(dimension: String, value: Boolean) {
        metricDimensions[dimension] = value
    }

    fun dimension(dimension: String, value: Long) {
        metricDimensions[dimension] = value
    }

    fun dimension(dimension: String, value: String) {
        metricDimensions[dimension] = value
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
