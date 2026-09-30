package com.walletapp.android.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PageResponse<T> {

    @SerializedName("content")
    private List<T> content;

    @SerializedName("total_elements")
    private long totalElements;

    @SerializedName("total_pages")
    private int totalPages;

    @SerializedName("page")
    private int page;

    @SerializedName("size")
    private int size;

    public List<T> getContent() {
        return content;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
