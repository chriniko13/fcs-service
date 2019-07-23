package com.chriniko.fc.statistics.resource;

import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.FieldStatistics;
import com.chriniko.fc.statistics.service.FieldConditionService;
import com.chriniko.fc.statistics.validator.FieldConditionInputValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@WebMvcTest

public class FieldConditionResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FieldConditionService fieldConditionService;

    @MockBean
    private FieldConditionInputValidator fieldConditionInputValidator;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void save() throws Exception {

        // given
        Instant occurrenceAt = Instant.now();
        FieldConditionCapture capture = new FieldConditionCapture(0.34, occurrenceAt);

        Mockito.doNothing().when(fieldConditionInputValidator).validate(capture);

        String payload = objectMapper.writeValueAsString(capture);


        // when - then
        mockMvc
                .perform(
                        post("/field-conditions")
                                .contentType("application/json")
                                .content(payload)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string("")
                );
    }

    @Test
    public void statistics() throws Exception {

        // given
        FieldStatistics statistics = new FieldStatistics();
        statistics.getVegetation().setMin(0.13);
        statistics.getVegetation().setMax(0.45);
        statistics.getVegetation().setAvg(0.32);

        Mockito.when(fieldConditionService.getStatistics())
                .thenReturn(statistics);

        String statisticsAsString = objectMapper.writeValueAsString(statistics);


        // when - then
        mockMvc
                .perform(
                        get("/field-conditions")
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content().string(statisticsAsString)
                );
    }
}