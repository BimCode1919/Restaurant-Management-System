package com.restaurant.qrorder.domain.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PresignedUrlResponse {

    /** Presigned PUT URL — frontend dùng URL này để upload file trực tiếp lên MinIO */
    private String presignedUrl;

    /** Public URL — URL công khai để truy cập ảnh sau khi upload xong */
    private String publicUrl;

    /** Thời gian hết hạn của presigned URL (phút) */
    private int expiresInMinutes;
}
