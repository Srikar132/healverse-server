package com.bytehealers.healverse.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImageUploadResponse {
    private boolean success;
    private String message;
    private String imageUrl;
    private String publicId;
}
