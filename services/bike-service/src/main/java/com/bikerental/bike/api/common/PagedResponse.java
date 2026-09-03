package com.bikerental.bike.api.common;

import org.springframework.data.domain.Page;

import java.util.List;

public record PagedResponse<T>(
     List<T> content,
     int page,
     int pageSize,
     long totalElements,
     int totalPages
) {

    public static <T> PagedResponse<T> of(Page<T> page) {
        return new PagedResponse<>(
                page.toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
