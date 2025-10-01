package com.bisttrading.core.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Generic paged response wrapper for paginated API responses.
 *
 * @param <T> The type of content items
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {

    /**
     * The list of items for the current page.
     */
    private List<T> content;

    /**
     * Current page number (0-based).
     */
    private int page;

    /**
     * Number of items per page.
     */
    private int size;

    /**
     * Total number of elements across all pages.
     */
    private long totalElements;

    /**
     * Total number of pages.
     */
    private int totalPages;

    /**
     * Number of elements in the current page.
     */
    private int numberOfElements;

    /**
     * Whether this is the first page.
     */
    private boolean first;

    /**
     * Whether this is the last page.
     */
    private boolean last;

    /**
     * Whether there are more pages after this one.
     */
    private boolean hasNext;

    /**
     * Whether there are pages before this one.
     */
    private boolean hasPrevious;

    /**
     * Whether the page is empty.
     */
    private boolean empty;

    /**
     * Sorting information.
     */
    private SortInfo sort;

    /**
     * Additional metadata about the page.
     */
    private PageMetadata metadata;

    /**
     * Creates a PagedResponse from Spring Data Page.
     *
     * @param page Spring Data Page
     * @param <T>  The type of content
     * @return PagedResponse
     */
    public static <T> PagedResponse<T> of(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .numberOfElements(page.getNumberOfElements())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .empty(page.isEmpty())
                .sort(SortInfo.of(page.getSort()))
                .build();
    }

    /**
     * Creates a PagedResponse with custom content and pagination info.
     *
     * @param content       The page content
     * @param pageable      Pagination parameters
     * @param totalElements Total number of elements
     * @param <T>           The type of content
     * @return PagedResponse
     */
    public static <T> PagedResponse<T> of(List<T> content, Pageable pageable, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        boolean isFirst = pageable.getPageNumber() == 0;
        boolean isLast = pageable.getPageNumber() >= totalPages - 1;

        return PagedResponse.<T>builder()
                .content(content)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .numberOfElements(content.size())
                .first(isFirst)
                .last(isLast)
                .hasNext(!isLast)
                .hasPrevious(!isFirst)
                .empty(content.isEmpty())
                .sort(SortInfo.of(pageable.getSort()))
                .build();
    }

    /**
     * Creates an empty PagedResponse.
     *
     * @param pageable Pagination parameters
     * @param <T>      The type of content
     * @return Empty PagedResponse
     */
    public static <T> PagedResponse<T> empty(Pageable pageable) {
        return PagedResponse.<T>builder()
                .content(List.of())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(0)
                .totalPages(0)
                .numberOfElements(0)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .empty(true)
                .sort(SortInfo.of(pageable.getSort()))
                .build();
    }

    /**
     * Creates a single page response (no pagination).
     *
     * @param content The content list
     * @param <T>     The type of content
     * @return PagedResponse with single page
     */
    public static <T> PagedResponse<T> singlePage(List<T> content) {
        return PagedResponse.<T>builder()
                .content(content)
                .page(0)
                .size(content.size())
                .totalElements(content.size())
                .totalPages(1)
                .numberOfElements(content.size())
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .empty(content.isEmpty())
                .sort(SortInfo.unsorted())
                .build();
    }

    /**
     * Adds metadata to the response.
     *
     * @param metadata Page metadata
     * @return This PagedResponse for method chaining
     */
    public PagedResponse<T> withMetadata(PageMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Checks if there are any elements in the page.
     *
     * @return true if content is not empty
     */
    public boolean hasContent() {
        return content != null && !content.isEmpty();
    }

    /**
     * Gets the page number in 1-based format.
     *
     * @return 1-based page number
     */
    public int getPageNumber1Based() {
        return page + 1;
    }

    /**
     * Calculates the starting element number for this page (1-based).
     *
     * @return Starting element number
     */
    public long getStartElementNumber() {
        return empty ? 0 : (long) page * size + 1;
    }

    /**
     * Calculates the ending element number for this page (1-based).
     *
     * @return Ending element number
     */
    public long getEndElementNumber() {
        return empty ? 0 : getStartElementNumber() + numberOfElements - 1;
    }

    /**
     * Sorting information for the page.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SortInfo {

        /**
         * Whether the result is sorted.
         */
        private boolean sorted;

        /**
         * Whether the result is unsorted.
         */
        private boolean unsorted;

        /**
         * Whether the sort is empty.
         */
        private boolean empty;

        /**
         * List of sort orders.
         */
        private List<SortOrder> orders;

        /**
         * Creates SortInfo from Spring Data Sort.
         *
         * @param sort Spring Data Sort
         * @return SortInfo
         */
        public static SortInfo of(org.springframework.data.domain.Sort sort) {
            if (sort.isUnsorted()) {
                return unsorted();
            }

            List<SortOrder> orders = sort.stream()
                    .map(order -> SortOrder.builder()
                            .property(order.getProperty())
                            .direction(order.getDirection().name())
                            .ascending(order.isAscending())
                            .descending(order.isDescending())
                            .ignoreCase(order.isIgnoreCase())
                            .build())
                    .toList();

            return SortInfo.builder()
                    .sorted(true)
                    .unsorted(false)
                    .empty(false)
                    .orders(orders)
                    .build();
        }

        /**
         * Creates an unsorted SortInfo.
         *
         * @return Unsorted SortInfo
         */
        public static SortInfo unsorted() {
            return SortInfo.builder()
                    .sorted(false)
                    .unsorted(true)
                    .empty(true)
                    .orders(List.of())
                    .build();
        }
    }

    /**
     * Individual sort order information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SortOrder {

        /**
         * The property being sorted.
         */
        private String property;

        /**
         * Sort direction (ASC or DESC).
         */
        private String direction;

        /**
         * Whether this is ascending sort.
         */
        private boolean ascending;

        /**
         * Whether this is descending sort.
         */
        private boolean descending;

        /**
         * Whether case is ignored.
         */
        private boolean ignoreCase;
    }

    /**
     * Additional metadata for the page.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageMetadata {

        /**
         * Processing time for the query in milliseconds.
         */
        private Long queryTimeMs;

        /**
         * Total count time in milliseconds.
         */
        private Long countTimeMs;

        /**
         * Whether the count is approximate.
         */
        private Boolean approximateCount;

        /**
         * Filter information applied to the query.
         */
        private Object filters;

        /**
         * Search query parameters.
         */
        private Object searchParams;

        /**
         * Additional custom metadata.
         */
        private Object additionalData;

        /**
         * Creates metadata with query time.
         *
         * @param queryTimeMs Query execution time
         * @return PageMetadata
         */
        public static PageMetadata withQueryTime(Long queryTimeMs) {
            return PageMetadata.builder()
                    .queryTimeMs(queryTimeMs)
                    .build();
        }

        /**
         * Creates metadata with query and count times.
         *
         * @param queryTimeMs Query execution time
         * @param countTimeMs Count execution time
         * @return PageMetadata
         */
        public static PageMetadata withTimes(Long queryTimeMs, Long countTimeMs) {
            return PageMetadata.builder()
                    .queryTimeMs(queryTimeMs)
                    .countTimeMs(countTimeMs)
                    .build();
        }
    }
}