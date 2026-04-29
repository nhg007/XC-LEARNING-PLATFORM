package com.xc.study.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PageParamsTests {

    @Test
    void normalizeUsesFixedPageSizeBuckets() {
        assertEquals(new PageParams(1, 20), PageParams.normalize(0, 0));
        assertEquals(new PageParams(1, 10), PageParams.normalize(1, 6));
        assertEquals(new PageParams(2, 20), PageParams.normalize(2, 17));
        assertEquals(new PageParams(3, 50), PageParams.normalize(3, 23));
        assertEquals(new PageParams(4, 100), PageParams.normalize(4, 101));
    }
}
