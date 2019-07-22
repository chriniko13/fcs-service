package com.chriniko.fc.statistics.dto;

import com.chriniko.fc.statistics.serde.InstantDeserializer;
import com.chriniko.fc.statistics.serde.InstantSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Data
@EqualsAndHashCode(of = {"occurrenceAt"})
@AllArgsConstructor
@NoArgsConstructor
public class FieldConditionCapture {

    @NotNull
    private Double vegetation;

    @NotNull
    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonSerialize(using = InstantSerializer.class)
    private Instant occurrenceAt;
}
