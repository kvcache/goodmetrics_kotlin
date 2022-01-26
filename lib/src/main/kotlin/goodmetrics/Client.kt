package goodmetrics

import io.goodmetrics.MetricsGrpcKt
import io.goodmetrics.datum
import io.goodmetrics.dimension
import io.goodmetrics.histogram
import io.goodmetrics.measurement
import io.goodmetrics.metricsRequest
import io.goodmetrics.statisticSet
import io.grpc.ManagedChannelBuilder
import io.grpc.netty.GrpcSslContexts
import io.grpc.netty.NettyChannelBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory

class Client private constructor(
    private val stub: MetricsGrpcKt.MetricsCoroutineStub,
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

    suspend fun sendMetrics() {
        stub.sendMetrics(
            metricsRequest {
                sharedDimensions["host"] = dimension { string = "my_laptop" }
                metrics.add(datum {
                    metric = "foo"
                    unixNanos = System.currentTimeMillis() * 1000000
                    dimensions["ordinal"] = dimension { number = 16 }
                    dimensions["possible"] = dimension { boolean = true }
                    measurements["some_value"] = measurement { gauge = 42.125 }
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
