package goodmetrics

enum class TimestampAt {
    // Stamp the metric at the start
    Start,
    // Stamp the metric at the end
    End,
}

class MetricsConfiguration private constructor() {
    companion object {
        var epochMillis: () -> Long = System::currentTimeMillis
    }
}

class GMetrics(
    stampAt: TimestampAt = TimestampAt.Start,
) {
    private val startTime: Long = when (stampAt) {
        TimestampAt.Start -> MetricsConfiguration.epochMillis()
        TimestampAt.End -> -1
    }
    private val intMeasurements: MutableMap<String, Long> = mutableMapOf()

    fun setIntMeasurement(measurement: String, value: Int) {
        intMeasurements[measurement] = value.toLong()
    }

    fun setLongMeasurement(measurement: String, value: Long) {
        intMeasurements[measurement] = value
    }
}
