package com.chriniko.fc.statistics.resource;


import com.chriniko.fc.statistics.dto.FieldConditionCapture;
import com.chriniko.fc.statistics.dto.FieldStatistics;
import com.chriniko.fc.statistics.service.FieldConditionService;
import com.chriniko.fc.statistics.validator.FieldConditionInputValidator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(value = "FieldConditionResource", description = "Field conditions operations (such as save field condition capture, statistics, etc)")


@RestController
@RequestMapping("/field-conditions")
public class FieldConditionResource {

    private final FieldConditionService fieldConditionService;
    private final FieldConditionInputValidator fieldConditionInputValidator;

    @Autowired
    public FieldConditionResource(FieldConditionService fieldConditionService,
                                  FieldConditionInputValidator fieldConditionInputValidator) {
        this.fieldConditionService = fieldConditionService;
        this.fieldConditionInputValidator = fieldConditionInputValidator;
    }

    @ApiOperation(value = "Save field condition capture")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully saved field condition capture")
            }
    )
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE
    )
    public @ResponseBody
    HttpEntity<Void> save(@RequestBody @Valid FieldConditionCapture input) {
        fieldConditionInputValidator.validate(input);
        fieldConditionService.store(input);
        return ResponseEntity.ok().build();
    }


    @ApiOperation(value = "Get field condition statistics related to the past 30 days", response = FieldStatistics.class)
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Successfully return field condition statistics related to the past 30 days")
            }
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    HttpEntity<FieldStatistics> statistics() {
        FieldStatistics statistics = fieldConditionService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
