package com.bytehealers.healverse.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TextRequest {
    @NotBlank(message = "Text cannot be empty")
    @Size(max = 1000, message = "Text cannot exceed 1000 characters")
    private String text;

    public TextRequest() {}

    public TextRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}