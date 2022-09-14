# <img src="https://user-images.githubusercontent.com/3454741/151748581-1ad6c34c-f583-4813-b878-d19c98ec3427.png" width="108em" align="center"/> Goodmetrics: Kotlin

This is the way (to record metrics)

# About
This is the Kotlin metrics client. It uses kotlinx coroutines to manage background
network interactions; you'll need to at least attach it to a framework-provided
dispatcher if you don't use coroutines already. If you do then you probably know
where you want to put the long-lived background job that emits metrics for your app.

# How to use
Run [the goodmetrics server](https://github.com/kvc0/goodmetrics) on localhost.

## Add dependency
```kotlin
dependencies {
    implementation("com.kvc0:goodmetrics-kotlin:$goodmetricsVersion")
}
```

## Write code
```kotlin
fun main() {
    val metricsBackgroundScope = CoroutineScope(Dispatchers.Default)
    val (emitterJob, metricsFactory) = metricsBackgroundScope.normalConfig()

    for (i in 1..1000) {
        // I want to record information about each iteration of the loop so I'll record {} each iteration.
        metricsFactory.record("demo_app") { metrics ->
            metrics.measure("iteration", i)
            metrics.dimension("random_boolean", Random.nextBoolean())
            metrics.measure("random_float", Random.nextFloat())
            metrics.dimension("host", Inet4Address.getLocalHost().hostName)
            // for heatmaps and percentiles, record a distribution.
            // Note that metricsFactory.record {} automatically records a
            //  "totaltime" distribution by default.
            metrics.distribution("file_size", getSizeOfFile(i))
        }
    }
    metricsBackgroundScope.cancel()
}
```
