package com.chriniko.fc.statistics.resource;


import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.FieldStatistics;
import com.chriniko.fc.statistics.service.FieldConditionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/field-conditions")
public class FieldConditionResource {

    private final FieldConditionService fieldConditionService;

    @Autowired
    public FieldConditionResource(FieldConditionService fieldConditionService) {
        this.fieldConditionService = fieldConditionService;
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public @ResponseBody
    HttpEntity<Void> record(@RequestBody @Valid FieldConditionCapture input) {

        fieldConditionService.store(input);
        return ResponseEntity.ok().build();
    }


    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    HttpEntity<FieldStatistics> statistics() {

        FieldStatistics statistics = fieldConditionService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
