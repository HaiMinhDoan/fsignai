package com.sunmoon.backend.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

// Khop dung shape BasicFetchResult<T> cua vben: { items, total }
// nen useTable() cua BasicTable dung duoc ngay khong can transform them.
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {

    List<T> items;
    long total;
    int page;
    int size;
    int totalPages;

    public static <E, D> PageResponse<D> of(Page<E> source, Function<E, D> mapper) {
        return PageResponse.<D>builder()
                .items(source.getContent().stream().map(mapper).toList())
                .total(source.getTotalElements())
                .page(source.getNumber())
                .size(source.getSize())
                .totalPages(source.getTotalPages())
                .build();
    }

    public static <D> PageResponse<D> of(Page<D> source) {
        return PageResponse.<D>builder()
                .items(source.getContent())
                .total(source.getTotalElements())
                .page(source.getNumber())
                .size(source.getSize())
                .totalPages(source.getTotalPages())
                .build();
    }
}
