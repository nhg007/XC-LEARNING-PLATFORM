package com.xc.study.common;

public record PageParams(long page, long pageSize) {

    private static final long DEFAULT_PAGE = 1;
    private static final long DEFAULT_PAGE_SIZE = 20;
    private static final long[] ALLOWED_PAGE_SIZES = {10, 20, 50, 100};

    public static PageParams normalize(long page, long pageSize) {
        return new PageParams(normalizePage(page), normalizePageSize(pageSize));
    }

    private static long normalizePage(long page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    private static long normalizePageSize(long pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        for (long allowedPageSize : ALLOWED_PAGE_SIZES) {
            if (pageSize <= allowedPageSize) {
                return allowedPageSize;
            }
        }
        return ALLOWED_PAGE_SIZES[ALLOWED_PAGE_SIZES.length - 1];
    }
}
