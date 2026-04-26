package com.xc.study;

import com.xc.study.common.ApiResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class StudyApiApplicationTests {

    @Test
    void apiResponseCanWrapData() {
        ApiResponse<String> response = ApiResponse.ok("ready");
        assertTrue(response.success());
        assertEquals("SUCCESS", response.code());
        assertEquals("ready", response.data());
    }
}
