package goodmetrics

/**
 * Not thread safe.
 */
class Metrics internal constructor(
    internal var name: String,
    internal var timestampMillis: Long,
) {
    internal val metricMeasurements: MutableMap<String, Any> = mutableMapOf()
    internal val metricDimensions: MutableMap<String, Any> = mutableMapOf()

    fun dimensionBool(dimension: String, value: Boolean) {
        metricDimensions[dimension] = value
    }

    fun dimensionNumber(dimension: String, value: Long) {
        metricDimensions[dimension] = value
    }

    fun dimensionString(dimension: String, value: String) {
        metricDimensions[dimension] = value
    }

    fun measureI(name: String, value: Long) {
        metricMeasurements[name] = value
    }

    fun measureF(name: String, value: Double) {
        metricMeasurements[name] = value
    }
}
