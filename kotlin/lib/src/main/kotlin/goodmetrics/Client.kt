package goodmetrics

import goodmetrics.pipeline.AggregatedBatch
import goodmetrics.pipeline.Aggregation
import io.goodmetrics.Datum
import io.goodmetrics.Dimension
import io.goodmetrics.Measurement
import io.goodmetrics.MetricsGrpcKt
import io.goodmetrics.datum
import io.goodmetrics.dimension
import io.goodmetrics.histogram
import io.goodmetrics.measurement
import io.goodmetrics.metricsRequest
import io.goodmetrics.statisticSet
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

class Client private constructor(
    private val stub: MetricsGrpcKt.MetricsCoroutineStub,
    private val prescientDimensions: Map<String, Dimension> = mapOf(),
) {
    companion object {
        fun connect(goodmetricsHostname: String = "localhost", port: Int = 9573): Client {
            val channel = NettyChannelBuilder.forAddress(goodmetricsHostname, port)
                .sslContext(
                    GrpcSslContexts.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build()
                )
                .build()
            return Client(MetricsGrpcKt.MetricsCoroutineStub(channel))
        }
    }

    suspend fun sendMetrics(batch: List<Metrics>) {
        val request = metricsRequest {
            sharedDimensions.putAll(prescientDimensions)
            batch.forEach {
                metrics.add(it.toProto())
            }
        }
        stub.sendMetrics(request)
    }

    suspend fun sendPreaggregatedMetrics(aggregatedBatches: List<AggregatedBatch>) {
        val request = metricsRequest {
            sharedDimensions.putAll(prescientDimensions)
            for (aggregatedBatch in aggregatedBatches) {
                for ((dimensionPosition, measurementMap) in aggregatedBatch.positions) {
                    val templateDatum = dimensionPosition.initializeDatum(aggregatedBatch.timestampNanos, aggregatedBatch.metric)
                    for ((measurement, aggregation) in measurementMap) {
                        templateDatum.measurementsMap[measurement] = aggregation.toProto()
                    }
                }
            }
        }
        stub.sendMetrics(request)
    }

    suspend fun sendToyMetrics() {
        stub.sendMetrics(
            metricsRequest {
                sharedDimensions["host"] = dimension { string = "my_laptop" }
                metrics.add(datum {
                    metric = "foo"
                    unixNanos = System.currentTimeMillis() * 1000000
                    dimensions["ordinal"] = dimension { number = 16 }
                    dimensions["possible"] = dimension { boolean = true }
                    measurements["some_ivalue"] = measurement { i32 = 42 }
                    measurements["some_fvalue"] = measurement { f64 = 42.125 }
                    measurements["some_stat"] = measurement { statisticSet = statisticSet {
                        minimum = 1.0
                        maximum = 2.0
                        samplesum = 4.0
                        samplecount = 3
                    } }
                    measurements["some_histgram"] = measurement { histogram = histogram {
                        buckets[0] = 1
                        buckets[1] = 3
                        buckets[2] = 5
                        buckets[3] = 5
                        buckets[4] = 5
                        buckets[5] = 5
                        buckets[6] = 5
                        buckets[16] = 9
                        buckets[17] = 9
                        buckets[18] = 9
                        buckets[19] = 9
                        buckets[20] = 9
                    } }
                })
            }
        )
    }
}

internal fun Metrics.toProto(): Datum = datum {
    metric = name
    unixNanos = timestampMillis * 1000000

    for ((k, v) in metricDimensions) {
        dimensions[k] = v.asDimension()
    }

    for ((k, v) in metricMeasurements) {
        measurements[k] = when (v) {
            is Long -> {
                measurement { i64 = v }
            }
            is Double -> {
                measurement { f64 = v }
            }
            // TODO: The preaggregated types
            else -> {
                throw IllegalArgumentException("unhandled measurement type: %s".format(v.javaClass.name))
            }
        }
    }

    for ((k, v) in metricDistributions) {
        measurements[k] = measurement { i64 = v }
    }
    measurements
}

fun Any.asDimension(): Dimension = when (this) {
    is Boolean -> {
        dimension { boolean = this@asDimension }
    }
    is Long -> {
        dimension { number = this@asDimension }
    }
    is String -> {
        dimension { string = this@asDimension }
    }
    else -> {
        throw IllegalArgumentException("unhandled dimension type: %s".format(this.javaClass.name))
    }
}

fun Set<Map.Entry<String, Any>>.initializeDatum(timestampNanos: Long, name: String): Datum = datum {
    unixNanos = timestampNanos
    metric = name
    for ((metric, position) in this@initializeDatum) {
        dimensions[metric] = position.asDimension()
    }
}

fun Aggregation.toProto(): Measurement = measurement {
    when (this@toProto) {
        is Aggregation.Histogram -> {
            histogram = histogram {
                for ((bucket, count) in this@toProto.bucketCounts) {
                    buckets[bucket] = count.sum()
                }
            }
        }
        is Aggregation.StatisticSet -> {
            statisticSet = statisticSet {
                minimum = min.get()
                maximum = max.get()
                samplesum = sum.sum()
                samplecount = count.sum()
            }
        }
    }
}
