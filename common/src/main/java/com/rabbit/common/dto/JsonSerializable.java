package com.rabbit.common.dto;

public interface JsonSerializable {
    /**
     * Converts this object to its JSON representation.
     *
     * @return JSON string
     */
    String toJson();
}