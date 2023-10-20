package no.liflig

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Scope
import java.io.File
import java.time.Instant
import kotlinx.serialization.json.JsonPrimitive

fun main(args: Array<String>) {
  println("Args: " + args.joinToString(separator = ", "))
  require(args.size == 1) { "Provide a file path as the first argument" }

  val logFile = File(args.first())
  require(logFile.isFile) { "The path must be a file: ${logFile.absolutePath}" }

  val events = RumLogs.load(logFile).events()
  val eventsBySession =
      events
          .filter { it.event_type != "com.amazon.rum.performance_resource_event" }
          .groupBy { it.user_details.sessionId }

  for (session in eventsBySession) {
    println("Session [${session.key}]")
    val eventsAscending = session.value.sortedBy { it.event_timestamp }

    check(Span.current() == Span.getInvalid()) {
      "Expected no current span. Span is set to ${Span.current()}"
    }

    traceSession(eventsAscending)
  }

  tracerProvider.apply {
    forceFlush()
    shutdown()
  }
  println("Done")
  Thread.sleep(1000)
}

private fun traceSession(eventsAscending: List<RumEvent>) {
  span(
      "RUM",
      start = eventsAscending.first().start(),
      end = eventsAscending.last().start().plusMillis(1),
      root = true) {
        var tracedPage: TracedPage? = null

        for ((index, rumEvent) in eventsAscending.withIndex()) {
          val page = rumEvent.metadata["pageId"] ?: ""
          if (tracedPage?.pageId != page) {
            tracedPage?.end(rumEvent.start())
            tracedPage = TracedPage.from(page, rumEvent.start())
          }

          val next = eventsAscending.getOrNull(index + 1)?.start() ?: rumEvent.start().plusMillis(1)

          val spanName =
              if (rumEvent.event_type == "no.tavler.rtk_event") {
                "${rumEvent.event_type} ${rumEvent.event_details?.get("type")}"
              } else {
                rumEvent.event_type
              }
          span(spanName, start = rumEvent.start(), end = next) {
            val attributesBuilder = Attributes.builder()
            rumEvent.metadata.forEach { attributesBuilder.put("rum.metadata.${it.key}", it.value) }
            rumEvent.event_details?.forEach {
              val value = it.value
              if (value is JsonPrimitive) {
                attributesBuilder.put("rum.eventDetails.${it.key}", value.content)
              }
            }

            attributesBuilder.put("rum.userDetails.userId", rumEvent.user_details.userId)
            attributesBuilder.put("rum.userDetails.sessionId", rumEvent.user_details.sessionId)

            Span.current().setAllAttributes(attributesBuilder.build())

            if (rumEvent.event_type == "com.amazon.rum.js_error_event") {
              Span.current()
                  .setStatus(
                      StatusCode.ERROR,
                      "${rumEvent.event_details?.get("type")}: ${rumEvent.event_details?.get("message")}")
            }
          }
        }

        tracedPage?.end(eventsAscending.last().start().plusMillis(1))
      }
}

class TracedPage(val pageId: String, private val span: Span, private val scope: Scope) {
  fun end(time: Instant) {
    scope.close()
    span.end(time)
  }

  companion object {
    fun from(page: String, start: Instant): TracedPage {
      val span = tracer.spanBuilder("Page $page").setStartTimestamp(start).startSpan()
      val scope = span.makeCurrent()
      return TracedPage(page, span, scope)
    }
  }
}
