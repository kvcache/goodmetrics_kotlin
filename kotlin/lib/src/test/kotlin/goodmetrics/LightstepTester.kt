package goodmetrics

import goodmetrics.downstream.GrpcTrailerLoggerInterceptor
import goodmetrics.downstream.OpentelemetryClient
import goodmetrics.downstream.PrescientDimensions
import goodmetrics.downstream.SecurityMode
import goodmetrics.pipeline.Aggregator
import goodmetrics.pipeline.SynchronizingBuffer
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.*
import java.net.InetAddress
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

fun main() {
    val lightstepIngestHost = "ingest.lightstep.com"
    val lightstepIngestPort = 443
    val lightstepAuthHeader = "lightstep-access-token"
    val lightstepToken = "redacted"

    val authHeader = io.grpc.Metadata()
    authHeader.put(io.grpc.Metadata.Key.of(lightstepAuthHeader, io.grpc.Metadata.ASCII_STRING_MARSHALLER), lightstepToken)

    val trailersInterceptor = GrpcTrailerLoggerInterceptor { status, trailers ->
        println("got trailers. Status: $status, Trailers: $trailers")
    }

    val client = OpentelemetryClient.connect(
        sillyOtlpHostname = lightstepIngestHost,
        port = lightstepIngestPort,
        prescientDimensions = PrescientDimensions.AsResource(
            mapOf(
                "service.name" to Metrics.Dimension.String("service.name", "goodmetrics_test"),
                "service.version" to Metrics.Dimension.String("service.version", OpentelemetryClient::class.java.`package`.implementationVersion ?: "dev"),
                "host.hostname" to Metrics.Dimension.String("host.hostname", InetAddress.getLocalHost().hostName)
            )
        ),
        securityMode = SecurityMode.Insecure,
        interceptors = listOf(
            MetadataUtils.newAttachHeadersInterceptor(authHeader),
            trailersInterceptor
        )
    )

    runBlocking {
        launch {
            runUnaryExample(client)
        }
        launch {
            runPreaggregatedExample(client)
        }
    }
}

private suspend fun runPreaggregatedExample(client: OpentelemetryClient) = coroutineScope {
    val sink = Aggregator(aggregationWidth = 1.seconds)
    val preaggregatedFactory = MetricsFactory(
        sink = sink,
        timeSource = NanoTimeSource.fastNanoTime,
        totaltimeType = MetricsFactory.TotaltimeType.DistributionMicroseconds,
    )

    launch {
        // Launch the sender on a background coroutine.
        sink.consume()
            .collect { metrics ->
                println("sending $metrics")
                try {
                    client.sendPreaggregatedBatch(
                        listOf(metrics)
                    )
                } catch (e: Exception) {
                    println("it broke while sending: $e")
                    e.printStackTrace()
                }
            }
    }

    var i = 0L
    var timestamp = TimeSource.Monotonic.markNow()
    val targetPeriod = 10.milliseconds
    while(true) {
        // Run a high frequency service api. We'll call it high_frequency_api
        ++i
        try {
            preaggregatedFactory.record("high_frequency_api") { metrics ->
                metrics.dimension("a_dimension", i % 8)
                metrics.distribution("random", ThreadLocalRandom.current().nextLong(4, 6))

                metrics.measure("small_random", ThreadLocalRandom.current().nextLong(4, 40))
            }
            val waitTime = -((timestamp + targetPeriod).elapsedNow())
            if (waitTime.isPositive()) {
                delay(waitTime)
            } else {
                println(waitTime)
            }
            timestamp += targetPeriod
        } catch (e: Exception) {
            println("it broke while recording")
            e.printStackTrace()
        }
    }
}
private suspend fun runUnaryExample(client: OpentelemetryClient) = coroutineScope {
    val sink = SynchronizingBuffer()
    val unaryFactory = MetricsFactory(
        sink = sink,
        timeSource = NanoTimeSource.preciseNanoTime,
        totaltimeType = MetricsFactory.TotaltimeType.MeasurementMicroseconds,
    )

    launch {
        // Launch the sender on a background coroutine.
        sink.consume()
            .collect { metrics ->
                println("sending $metrics")
                try {
                    client.sendMetricsBatch(
                        listOf(metrics)
                    )
                } catch (e: Exception) {
                    println("it broke while sending: $e")
                    e.printStackTrace()
                }
            }
    }

    repeat(Int.MAX_VALUE) { i ->
        // Run a service api. We'll call it "example"
        try {
            unaryFactory.record("example") { metrics ->
                metrics.dimension("round", i.toLong() % 8)
                metrics.measure("random", ThreadLocalRandom.current().nextFloat(0.3f, 0.97f))
                delay(ThreadLocalRandom.current().nextLong(1, 13))
            }
        } catch (e: Exception) {
            println("it broke while recording")
            e.printStackTrace()
        }
        delay(200)
    }
}
