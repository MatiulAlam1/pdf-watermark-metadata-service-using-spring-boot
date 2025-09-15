/*
package com.valmet.watermark.controller;

import com.valmet.watermark.constants.Constants;
import com.valmet.watermark.enums.ResponseType;
import com.valmet.watermark.response.BaseResponse;
import com.valmet.watermark.service.PropertyUpdaterService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {
    private final PropertyUpdaterService propertyUpdaterService;

    public PropertyController(PropertyUpdaterService propertyUpdaterService) {
	this.propertyUpdaterService = propertyUpdaterService;
    }

    @PostMapping("/update")
    public BaseResponse updateProperty(@RequestParam String key, @RequestParam String value) throws IOException {
	return BaseResponse.builder().responseType(ResponseType.RESULT)
		.message(Collections.singleton(HttpStatus.OK.getReasonPhrase()))
		.result(propertyUpdaterService.updateProperty(key, value)).code(Constants.SUCCESS_CODE).build();
    }
}
*/
