package com.goldatech.notificationservice.web.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "eventType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = PaymentEvent.class, name = "PAYMENT"),
        @JsonSubTypes.Type(value = CollectionEvent.class, name = "COLLECTION"),
        @JsonSubTypes.Type(value = AuthEvent.class, name = "AUTH")
})
public interface DomainEvent extends Serializable {}