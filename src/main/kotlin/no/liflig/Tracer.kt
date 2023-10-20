package no.liflig

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import java.time.Instant
import java.util.logging.Level
import java.util.logging.Logger

val tracerProvider =
    SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(OtlpHttpSpanExporter.builder().build()))
        .addResource(
            Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "cwrum")))
        .build()

val tracer by lazy {
    Logger.getLogger("io.opentelemetry").level = Level.ALL

  OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal()

  GlobalOpenTelemetry.tracerBuilder("cwrum").setInstrumentationVersion("1.0.0").build()
}

fun span(
    name: String,
    start: Instant,
    end: Instant,
    root: Boolean = false,
    block: (() -> Any?)? = null
) {
  val span =
      tracer
          .spanBuilder(name)
          .setStartTimestamp(start)
          .apply {
            if (root) {
              setSpanKind(SpanKind.SERVER)
            }
          }
          .startSpan()
  try {
    span.makeCurrent().use {
      if (root) {
        println("Created trace ${span.spanContext.traceId}")
      }
      // println("Created span ${span.spanContext.spanId}")

      block?.invoke()
    }
  } finally {
    span.end(end)
  }
}
