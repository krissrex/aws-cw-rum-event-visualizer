package no.liflig

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import java.time.Instant

private val json = Json {
  ignoreUnknownKeys = true
  isLenient = true
}

@Serializable
class RumLogs(private val events: List<LogEvent>) {
  companion object {
    fun load(file: File): RumLogs {
      return json.decodeFromStream<RumLogs>(file.inputStream().buffered())
    }
  }

    fun events(): List<RumEvent> = events.map { it.toRumEvent() }
}

@Serializable
class LogEvent(private val message: String) {
  fun toRumEvent(): RumEvent = json.decodeFromString(message)
}

@Serializable
data class RumEvent(
    val event_timestamp: Long,
    val event_type: String,
    val metadata: Map<String, String>,
    val user_details: UserDetails,
    val event_details: JsonObject?
) {
    fun start(): Instant = Instant.ofEpochMilli(event_timestamp)
}

@Serializable
data class UserDetails(val sessionId: String, val userId: String)

/*
{
  "event_timestamp": 1695196227000,
  "event_type": "com.amazon.rum.session_start_event",
  "event_id": "a125ac43-652c-4799-9363-18bf7a5786f5",
  "event_version": "1.0.0",
  "log_stream": "2023-09-20T00",
  "application_id": "9c420d0e-23f2-4257-acff-ef95de04be4b",
  "application_version": "",
  "metadata": {
    "version": "1.0.0",
    "browserLanguage": "en-US",
    "browserName": "Firefox",
    "browserVersion": "117.0",
    "osName": "Linux",
    "osVersion": "x86_64",
    "deviceType": "desktop",
    "platformType": "web",
    "pageId": "/",
    "title": "Tavler Webapp",
    "domain": "localhost",
    "aws:client": "arw-module",
    "aws:clientVersion": "1.14.0",
    "appName": "tavler",
    "appBuildTime": "2023-09-20T07:50:17.421Z",
    "commitHash": "24b3b22",
    "branch": "feat/tilrettelegging-list-apperance",
    "countryCode": "NO",
    "subdivisionCode": "30"
  },
  "user_details": {
    "sessionId": "fc26f85c-f6e7-4751-91f4-ee9dfb8b4828",
    "userId": "284f2d11-bb75-4ba4-bab5-e83c06b01547"
  },
  "event_details": {
    "version": "1.0.0"
  }
}
 */
