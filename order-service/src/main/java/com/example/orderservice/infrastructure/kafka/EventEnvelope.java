package com.example.orderservice.infrastructure.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Generic envelope for deserializing incoming events. Payload is kept as JsonNode
 * so consumers can extract fields without binding to every event type.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventEnvelope {

    private String eventId;
    private String eventType;
    private String version;
    private String correlationId;
    private String timestamp;
    private JsonNode payload;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public JsonNode getPayload() { return payload; }
    public void setPayload(JsonNode payload) { this.payload = payload; }
}
