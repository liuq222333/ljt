package com.example.demo.demos.PageResponse;

/* PageResponse.java */
import java.util.List;

public class PageResponse<T> {
    private List<T> items;
    private int page;
    private int pageSize;
    private long total;

    public PageResponse(List<T> items, int page, int pageSize, long total) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
    }

    public PageResponse() {
    }

    /**
     * 获取
     * @return items
     */
    public List<T> getItems() {
        return items;
    }

    /**
     * 设置
     * @param items
     */
    public void setItems(List<T> items) {
        this.items = items;
    }

    /**
     * 获取
     * @return page
     */
    public int getPage() {
        return page;
    }

    /**
     * 设置
     * @param page
     */
    public void setPage(int page) {
        this.page = page;
    }

    /**
     * 获取
     * @return pageSize
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * 设置
     * @param pageSize
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * 获取
     * @return total
     */
    public long getTotal() {
        return total;
    }

    /**
     * 设置
     * @param total
     */
    public void setTotal(long total) {
        this.total = total;
    }

    public String toString() {
        return "PageResponse{items = " + items + ", page = " + page + ", pageSize = " + pageSize + ", total = " + total + "}";
    }
    // getters/setters ...
}