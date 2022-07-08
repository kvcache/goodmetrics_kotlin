package goodmetrics

/**
 * Not thread safe.
 */
data class Metrics internal constructor(
    internal val name: String,
    internal var timestampNanos: Long,
    internal val startNanoTime: Long,
) {
    sealed interface Dimension {
        val name: kotlin.String
        data class String(override val name: kotlin.String, val value: kotlin.String) : Dimension
        data class Number(override val name: kotlin.String, val value: Long) : Dimension
        data class Boolean(override val name: kotlin.String, val value: kotlin.Boolean) : Dimension
    }
    internal val metricMeasurements: MutableMap<String, Number> = mutableMapOf()
    internal val metricDistributions: MutableMap<String, Long> = mutableMapOf()
    internal val metricDimensions: MutableMap<String, Dimension> = mutableMapOf()

    data class View(
        val metricName: String,
        val timestampNanos: Long,
        val startNanoTime: Long,
        val dimensions: Map<String, Dimension>,
        val measurements: Map<String, Number>,
        val distributions: Map<String, Long>,
    )

    fun getView(): View {
        return View(
            name,
            timestampNanos,
            startNanoTime,
            metricDimensions.toMap(),
            metricMeasurements.toMap(),
            metricDistributions.toMap(),
        )
    }

    fun dimension(dimension: String, value: Boolean) {
        metricDimensions[dimension] = Dimension.Boolean(dimension, value)
    }

    fun dimension(dimension: String, value: Long) {
        metricDimensions[dimension] = Dimension.Number(dimension, value)
    }

    fun dimension(dimension: String, value: String) {
        metricDimensions[dimension] = Dimension.String(dimension, value)
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
