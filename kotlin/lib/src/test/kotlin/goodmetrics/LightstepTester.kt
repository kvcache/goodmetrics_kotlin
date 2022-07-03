package goodmetrics

import goodmetrics.downstream.OpentelemetryClient
import goodmetrics.downstream.PrescientDimensions
import goodmetrics.downstream.SecurityMode
import goodmetrics.pipeline.SynchronizingBuffer
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.net.InetAddress
import java.util.concurrent.ThreadLocalRandom

fun main() {
    val lightstepIngestHost = "ingest.lightstep.com"
    val lightstepIngestPort = 443
    val lightstepAuthHeader = "lightstep-access-token"
    val lightstepToken = "<redacted>"

    val authHeader = io.grpc.Metadata()
    authHeader.put(io.grpc.Metadata.Key.of(lightstepAuthHeader, io.grpc.Metadata.ASCII_STRING_MARSHALLER), lightstepToken)

    val client = OpentelemetryClient.connect(
        sillyOtlpHostname = lightstepIngestHost,
        port = lightstepIngestPort,
        prescientDimensions = PrescientDimensions.AsResource(
            mapOf(
                "service.name" to Metrics.Dimension.Str("service.name", "goodmetrics_test"),
                "service.version" to Metrics.Dimension.Str("service.version", OpentelemetryClient::class.java.`package`.implementationVersion ?: "dev"),
                "host.hostname" to Metrics.Dimension.Str("host.hostname", InetAddress.getLocalHost().hostName)
            )
        ),
        securityMode = SecurityMode.Insecure,
        interceptors = listOf(
            MetadataUtils.newAttachHeadersInterceptor(authHeader)
        )
    )
    val sink = SynchronizingBuffer()
    val factory = MetricsFactory(
        sink = sink,
        timeSource = NanoTimeSource.preciseNanoTime,
    )
    val sendingHowMany = 100
    val sentAll = Semaphore(sendingHowMany, sendingHowMany)

    runBlocking {
        val a = async {
            sink.consume()
                .collect { metrics ->
                    println("sending $metrics")
                    try {
                        client.sendMetricsBatch(
                            listOf(metrics)
                        )
                        sentAll.release()
                    } catch (e: Exception) {
                        println("it broke while sending: $e")
                        e.printStackTrace()
                    }
                }
        }
        repeat(100) { i ->
            try {
                factory.record("example") { metrics ->
                    metrics.dimension("round", i.toLong() % 8)
                    metrics.measure("random", ThreadLocalRandom.current().nextFloat(0.3f, 0.97f))
                    delay(ThreadLocalRandom.current().nextLong(1, 13))
                }
            } catch (e: Exception) {
                println("it broke while recording")
                e.printStackTrace()
            }
        }
        repeat(sendingHowMany) { sentAll.acquire() }
        a.cancelAndJoin()
    }
}
