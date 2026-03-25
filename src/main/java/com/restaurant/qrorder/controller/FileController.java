package com.restaurant.qrorder.controller;

import com.restaurant.qrorder.domain.dto.request.PresignedUrlRequest;
import com.restaurant.qrorder.domain.dto.response.ApiResponse;
import com.restaurant.qrorder.domain.dto.response.PresignedUrlResponse;
import com.restaurant.qrorder.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE)
@Tag(name = "File Upload", description = "APIs for generating presigned upload URLs")
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    final MinioService minioService;

    @Value("${minio.presigned-url-expiry}")
    int presignedUrlExpiry;

    @PostMapping("/presigned-url")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(
            summary = "Get presigned upload URL",
            description = """
                    Generate a presigned PUT URL for uploading a file directly to MinIO.

                    **Flow:**
                    1. Call this endpoint to get `presignedUrl` and `publicUrl`
                    2. Use `presignedUrl` to PUT the file directly (set Content-Type header to match)
                    3. Save `publicUrl` as the image URL (e.g., item.imageUrl)
                    """
    )
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        String[] urls = minioService.generatePresignedPutUrl(request.getFileName(), request.getContentType());

        PresignedUrlResponse response = PresignedUrlResponse.builder()
                .presignedUrl(urls[0])
                .publicUrl(urls[1])
                .expiresInMinutes(presignedUrlExpiry)
                .build();

        return ResponseEntity.ok(
                ApiResponse.<PresignedUrlResponse>builder()
                        .statusCode(200)
                        .message("Presigned URL generated successfully")
                        .data(response)
                        .build()
        );
    }
}
