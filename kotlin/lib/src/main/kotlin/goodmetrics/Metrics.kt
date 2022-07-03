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
        val name: String
        val value: Any
        data class Str(override val name: String, override val value: String) : Dimension
        data class Num(override val name: String, override val value: Long) : Dimension
        data class Bool(override val name: String, override val value: Boolean) : Dimension
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
        metricDimensions[dimension] = Dimension.Bool(dimension, value)
    }

    fun dimension(dimension: String, value: Long) {
        metricDimensions[dimension] = Dimension.Num(dimension, value)
    }

    fun dimension(dimension: String, value: String) {
        metricDimensions[dimension] = Dimension.Str(dimension, value)
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
