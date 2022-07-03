package goodmetrics.downstream

import goodmetrics.Metrics
import goodmetrics.pipeline.AggregatedBatch
import goodmetrics.pipeline.Aggregation
import goodmetrics.pipeline.bucket
import io.grpc.*
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.opentelemetry.proto.collector.metrics.v1.MetricsServiceGrpcKt
import io.opentelemetry.proto.collector.metrics.v1.exportMetricsServiceRequest
import io.opentelemetry.proto.common.v1.*
import io.opentelemetry.proto.metrics.v1.*
import io.opentelemetry.proto.resource.v1.resource


sealed interface PrescientDimensions {
    data class AsResource(val resourceDimensions: Map<String, Metrics.Dimension>) : PrescientDimensions
    data class AsDimensions(val sharedDimensions: Map<String, Metrics.Dimension>) : PrescientDimensions
}

enum class SecurityMode {
    Plaintext,
    Insecure,
    Tls,
}

/**
 * This client should be used as a last resort, in defeat, if you
 * cannot use the goodmetrics protocol. Opentelemetry is highly
 * lossy and inflexible. I'm doing my best here, but you're not
 * getting the full goodmetrics experience if you're still
 * addicted to opentelemetry line protocol.
 */
class OpentelemetryClient(
    channel: ManagedChannel,
    private val prescientDimensions: PrescientDimensions,
) {
    companion object {
        fun connect(
            sillyOtlpHostname: String = "localhost",
            port: Int = 5001,
            prescientDimensions: PrescientDimensions,
            securityMode: SecurityMode,
            /**
             * stuff like MetadataUtils.newAttachHeadersInterceptor()
             */
            interceptors: List<ClientInterceptor>,
        ): OpentelemetryClient {
            val channelBuilder = NettyChannelBuilder.forAddress(sillyOtlpHostname, port)
            when (securityMode) {
                SecurityMode.Tls -> {
                    channelBuilder.useTransportSecurity()
                }
                SecurityMode.Insecure -> {
                    channelBuilder.useTransportSecurity()
                    channelBuilder.sslContext(
                        GrpcSslContexts.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build()
                    )
                }
                SecurityMode.Plaintext -> {
                    channelBuilder.usePlaintext()
                }
            }
            channelBuilder.intercept(interceptors)
            return OpentelemetryClient(channelBuilder.build(), prescientDimensions)
        }
    }
    private val stub: MetricsServiceGrpcKt.MetricsServiceCoroutineStub = MetricsServiceGrpcKt.MetricsServiceCoroutineStub(channel)

    suspend fun sendMetricsBatch(batch: List<Metrics>) {
        stub.export(
            exportMetricsServiceRequest {
                resourceMetrics.add(
                    asResourceMetrics(batch)
                )
            }
        )
    }

    suspend fun sendPreaggregatedBatch(batch: List<AggregatedBatch>) {
        stub.export(
            exportMetricsServiceRequest {
                resourceMetrics.add(
                    asResourceMetricsFromBatch(batch)
                )
            }
        )
    }

    private fun asResourceMetricsFromBatch(batch: List<AggregatedBatch>): ResourceMetrics {
        return resourceMetrics {
            prescientResource?.let { this.resource = it }
            for (aggregate in batch) {
                this.instrumentationLibraryMetrics.add(aggregate.asOtlpScopeMetrics())
            }
        }
    }

    private fun AggregatedBatch.asOtlpScopeMetrics(): InstrumentationLibraryMetrics = instrumentationLibraryMetrics {
        instrumentationLibrary = library
        metrics.addAll(this@asOtlpScopeMetrics.asGoofyOtlpMetricSequence().asIterable())
    }

    private fun asResourceMetrics(batch: List<Metrics>): ResourceMetrics {
        return resourceMetrics {
            prescientResource?.let { this.resource = it }
            for (metric in batch) {
                instrumentationLibraryMetrics.add(asScopeMetrics(batch))
            }
        }
    }

    private fun asScopeMetrics(batch: List<Metrics>): InstrumentationLibraryMetrics = instrumentationLibraryMetrics {
        instrumentationLibrary = library
        metrics.addAll(batch.asSequence().flatMap { it.asGoofyOtlpMetricSequence() }.asIterable())
    }

    private fun AggregatedBatch.asGoofyOtlpMetricSequence(): Sequence<Metric> = sequence {
        for ((position, measurements) in this@asGoofyOtlpMetricSequence.positions) {
            val otlpDimensions = position.map { it.asOtlpStringKeyValue() }
            for ((measurementName, aggregation) in measurements) {
                yield(
                    metric {
                        name = "${this@asGoofyOtlpMetricSequence.metric}_$measurementName"
                        unit = "1"
                        when (aggregation) {
                            is Aggregation.Histogram -> {
                                doubleHistogram = doubleHistogram {
                                    aggregation.asOtlpHistogram(otlpDimensions, this@asGoofyOtlpMetricSequence.timestampNanos)
                                }
                            }
                            is Aggregation.StatisticSet -> TODO()
                        }
                    }
                )
            }
        }
    }

    private fun Metrics.asGoofyOtlpMetricSequence(): Sequence<Metric> {
        val otlpDimensions = metricDimensions.values.map { it.asOtlpStringKeyValue() }
        return sequence {
            for ((measurementName, value) in this@asGoofyOtlpMetricSequence.metricMeasurements) {
                yield(
                    metric {
                        // name: format!("{metric_name}_{measurement_name}", metric_name = datum.metric, measurement_name=name),
                        name = "${this@asGoofyOtlpMetricSequence.name}_$measurementName"
                        unit = "1"

                        if (value is Long) {
                            intGauge = intGauge {
                                this.dataPoints.add(intDataPoint {
                                    this.timeUnixNano = timestampNanos
                                    labels.addAll(otlpDimensions.asIterable())
                                    this.value = value
                                })
                            }
                        } else {
                            doubleGauge = doubleGauge {
                                this.dataPoints.add(doubleDataPoint {
                                    this.timeUnixNano = timestampNanos
                                    labels.addAll(otlpDimensions.asIterable())
                                    this.value = value.toDouble()
                                })
                            }
                        }
                    }
                )
            }
            for ((measurementName, value) in this@asGoofyOtlpMetricSequence.metricDistributions) {
                yield(
                    metric {
                        // name: format!("{metric_name}_{measurement_name}", metric_name = datum.metric, measurement_name=name),
                        name = "${this@asGoofyOtlpMetricSequence.name}_$measurementName"
                        unit = "1"
                        doubleHistogram = asOtlpHistogram(otlpDimensions, value)
                    }
                )
            }
        }
    }

    private fun Metrics.asOtlpHistogram(
        otlpDimensions: Iterable<StringKeyValue>,
        value: Long
    ) = doubleHistogram {
        // Because cumulative is bullshit for service metrics. Change my mind.
        aggregationTemporality = AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA
        dataPoints.add(doubleHistogramDataPoint {
            labels.addAll(otlpDimensions)
            startTimeUnixNano = 0
            timeUnixNano = timestampNanos
            count = 1
            sum = value.toDouble()
            explicitBounds.add(bucket(value).toDouble())
            bucketCounts.add(value)
        })
    }

    private fun Aggregation.Histogram.asOtlpHistogram(
        otlpDimensions: Iterable<StringKeyValue>,
        timestampNanos: Long,
    ) = doubleHistogram {
        // Because cumulative is bullshit for service metrics. Change my mind.
        aggregationTemporality = AggregationTemporality.AGGREGATION_TEMPORALITY_DELTA
        dataPoints.add(doubleHistogramDataPoint {
            labels.addAll(otlpDimensions)
            startTimeUnixNano = 0
            timeUnixNano = timestampNanos
            count = this@asOtlpHistogram.bucketCounts.values.sumOf { it.sum() }
            sum = this@asOtlpHistogram.bucketCounts.entries.sumOf { (bucket, count) -> bucket * count.sum() }.toDouble()
            explicitBounds.addAll(this@asOtlpHistogram.bucketCounts.keys.asSequence().map { it.toDouble() }.asIterable())
            bucketCounts.addAll(this@asOtlpHistogram.bucketCounts.values.map { it.sum() })
        })
    }

    private val library = instrumentationLibrary {
        name = "goodmetrics_kotlin"
        version = OpentelemetryClient::class.java.`package`.implementationVersion ?: "development"
    }

    private val prescientResource by lazy {
        when (prescientDimensions) {
            is PrescientDimensions.AsDimensions -> {
                null
            }
            is PrescientDimensions.AsResource -> {
                resource {
                    attributes.addAll(prescientDimensions.resourceDimensions.asOtlpDimensions().asIterable())
                }
            }
        }
    }

    private fun Map<String, Metrics.Dimension>.asOtlpDimensions(): Sequence<KeyValue> = sequence {
        for (dimension in this@asOtlpDimensions) {
            yield(dimension.value.asOtlpKeyValue())
        }
    }

    private fun Metrics.Dimension.asOtlpKeyValue(): KeyValue = keyValue {
        key = this@asOtlpKeyValue.name
        when(val v = this@asOtlpKeyValue.value) {
            is Metrics.Dimension.Bool -> {
                value = anyValue { boolValue = v.value }
            }
            is Metrics.Dimension.Num -> {
                value = anyValue { intValue = v.value }
            }
            is Metrics.Dimension.Str -> {
                value = anyValue { stringValue = v.value }
            }
        }
    }

    private fun Metrics.Dimension.asOtlpStringKeyValue(): StringKeyValue = stringKeyValue {
        key = this@asOtlpStringKeyValue.name
        when(val v = this@asOtlpStringKeyValue.value) {
            is Metrics.Dimension.Bool -> {
                value = v.value.toString()
            }
            is Metrics.Dimension.Num -> {
                value = v.value.toString()
            }
            is Metrics.Dimension.Str -> {
                value = v.value
            }
        }
    }
}
