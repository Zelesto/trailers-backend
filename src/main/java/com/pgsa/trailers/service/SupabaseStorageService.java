// src/main/java/com/pgsa/trailers/service/SupabaseStorageService.java
package com.pgsa.trailers.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.core.ResponseInputStream;

import jakarta.annotation.PostConstruct;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupabaseStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${supabase.s3.bucket}")
    private String bucketName;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.s3.endpoint}")
    private String endpoint;

    @Value("${supabase.s3.region}")
    private String region;

    private boolean isConfigured() {
        return StringUtils.hasText(endpoint)
                && StringUtils.hasText(bucketName)
                && StringUtils.hasText(supabaseUrl)
                && StringUtils.hasText(region);
    }

    /**
     * Test connection to Supabase bucket on startup
     */
    @PostConstruct
    public void testConnection() {
        if (!isConfigured()) {
            log.warn("Supabase storage is not configured; skipping startup connection check.");
            return;
        }

        log.info("========================================");
        log.info("🔌 Testing Supabase S3 Connection");
        log.info("========================================");
        log.info("Endpoint: {}", endpoint);
        log.info("Region: {}", region);
        log.info("Bucket: {}", bucketName);
        log.info("Supabase URL: {}", supabaseUrl);
        log.info("----------------------------------------");
        
        try {
            // Test 1: Check if bucket exists
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            
            HeadBucketResponse response = s3Client.headBucket(headBucketRequest);
            log.info("✅ Bucket '{}' exists and is accessible", bucketName);
            log.info("   Response: {}", response);
            
            // Test 2: List objects in bucket (limit to 1 to verify access)
            ListObjectsRequest listRequest = ListObjectsRequest.builder()
                    .bucket(bucketName)
                    .maxKeys(1)
                    .build();
            
            ListObjectsResponse listResponse = s3Client.listObjects(listRequest);
            log.info("✅ Can list objects in bucket '{}'", bucketName);
            log.info("   Object count: {}", listResponse.contents().size());
            
            log.info("========================================");
            log.info("✅ Supabase Storage connection successful!");
            log.info("========================================");
            
        } catch (NoSuchBucketException e) {
            log.error("❌ Bucket '{}' does not exist or is not accessible", bucketName, e);
            log.error("   Please create the bucket in Supabase Storage");
            log.error("   Error: {}", e.getMessage());
        } catch (S3Exception e) {
            log.error("❌ Failed to connect to Supabase S3: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            log.error("   Error Message: {}", e.awsErrorDetails().errorMessage());
        } catch (Exception e) {
            log.error("❌ Unexpected error connecting to Supabase: {}", e.getMessage(), e);
        }
    }

    /**
     * Upload and convert file to PDF
     */
    public String uploadAndConvertFile(MultipartFile file, String podNumber, FileConversionService conversionService) {
        if (!isConfigured()) {
            throw new IllegalStateException("Supabase storage is not configured. Set SUPABASE_S3_ENDPOINT, SUPABASE_ACCESS_KEY, SUPABASE_SECRET_KEY, and SUPABASE_BUCKET to enable uploads.");
        }

        log.info("Processing and uploading file for POD: {}", podNumber);
        
        try {
            // Log file details
            String originalFilename = file.getOriginalFilename();
            String contentType = file.getContentType();
            String extension = getFileExtension(originalFilename);
            
            log.info("File details - Name: {}, Type: {}, Extension: {}, Size: {} bytes", 
                originalFilename, contentType, extension, file.getSize());
            
            byte[] fileData;
            String finalFilename;
            String finalContentType;
            
            // If not PDF, convert to PDF
            if (!"pdf".equalsIgnoreCase(extension) && !"application/pdf".equalsIgnoreCase(contentType)) {
                log.info("Converting {} to PDF", originalFilename);
                try {
                    fileData = conversionService.convertToPdf(file);
                    finalFilename = podNumber + ".pdf";
                    finalContentType = "application/pdf";
                    log.info("Conversion successful. PDF size: {} bytes", fileData.length);
                } catch (Exception e) {
                    log.error("Failed to convert file to PDF: {}", e.getMessage(), e);
                    // If conversion fails, try uploading the original file
                    log.info("Attempting to upload original file as fallback");
                    fileData = file.getBytes();
                    finalFilename = podNumber + "." + extension;
                    finalContentType = contentType != null ? contentType : "application/octet-stream";
                }
            } else {
                fileData = file.getBytes();
                finalFilename = podNumber + "." + extension;
                finalContentType = contentType != null ? contentType : "application/pdf";
            }
            
            // Use simple path without subfolder
            String filePath = finalFilename;
            
            log.info("Uploading file to Supabase: bucket={}, path={}", bucketName, filePath);
            log.info("File size: {} bytes, Content-Type: {}", fileData.length, finalContentType);
            
            // Upload to Supabase
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(finalContentType)
                    .contentLength((long) fileData.length)
                    .build();
            
            long startTime = System.currentTimeMillis();
            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(fileData)
            );
            long endTime = System.currentTimeMillis();
            
            log.info("✅ File uploaded successfully in {} ms", (endTime - startTime));
            log.info("   ETag: {}", response.eTag());
            
            // Generate the public URL
            String fileUrl = String.format("%s/storage/v1/object/public/%s/%s", 
                    supabaseUrl, bucketName, filePath);
            
            log.info("   File URL: {}", fileUrl);
            log.info("========================================");
            return fileUrl;
            
        } catch (IOException e) {
            log.error("❌ IO Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read file: " + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("❌ Supabase S3 Error: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            log.error("   Error Message: {}", e.awsErrorDetails().errorMessage());
            log.error("   Request ID: {}", e.requestId());
            throw new RuntimeException("Supabase upload failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload a file to Supabase Storage with detailed logging
     */
    public String uploadFile(MultipartFile file, String podNumber) {
        if (!isConfigured()) {
            throw new IllegalStateException("Supabase storage is not configured. Set SUPABASE_S3_ENDPOINT, SUPABASE_ACCESS_KEY, SUPABASE_SECRET_KEY, and SUPABASE_BUCKET to enable uploads.");
        }

        log.info("========================================");
        log.info("📤 Starting file upload");
        log.info("========================================");
        log.info("Pod Number: {}", podNumber);
        log.info("File Name: {}", file.getOriginalFilename());
        log.info("File Size: {} bytes", file.getSize());
        log.info("File Content Type: {}", file.getContentType());
        
        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString();
            String filePath = String.format("%s/%s.%s", podNumber, fileName, extension);
            
            log.info("Generated file path: {}", filePath);
            log.info("Target bucket: {}", bucketName);
            
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            
            log.info("Uploading to Supabase...");
            long startTime = System.currentTimeMillis();
            
            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            
            long endTime = System.currentTimeMillis();
            log.info("✅ File uploaded successfully in {} ms", (endTime - startTime));
            log.info("   ETag: {}", response.eTag());
            log.info("   Version ID: {}", response.versionId());
            
            String fileUrl = String.format("%s/storage/v1/object/public/%s/%s", 
                    supabaseUrl, bucketName, filePath);
            
            log.info("   File URL: {}", fileUrl);
            log.info("========================================");
            return fileUrl;
            
        } catch (IOException e) {
            log.error("❌ IO Error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("❌ Supabase S3 Error: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            log.error("   Error Message: {}", e.awsErrorDetails().errorMessage());
            log.error("   Request ID: {}", e.requestId());
            throw new RuntimeException("Supabase upload failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error uploading file: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Download a file from Supabase Storage
     */
    public byte[] downloadFile(String fileUrl) {
        if (!isConfigured()) {
            throw new IllegalStateException("Supabase storage is not configured. Set SUPABASE_S3_ENDPOINT, SUPABASE_ACCESS_KEY, SUPABASE_SECRET_KEY, and SUPABASE_BUCKET to enable downloads.");
        }

        try {
            log.info("📥 Downloading file from: {}", fileUrl);
            
            if (fileUrl == null || fileUrl.isEmpty()) {
                log.error("❌ File URL is null or empty");
                return null;
            }
            
            // Extract file path from URL
            String filePath = extractFilePath(fileUrl);
            log.info("📥 Extracted path: {}", filePath);
            
            if (filePath == null || filePath.isEmpty()) {
                log.error("❌ Failed to extract file path from URL: {}", fileUrl);
                return null;
            }
            
            // Download from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            log.info("📥 Downloading from S3: bucket={}, key={}", bucketName, filePath);
            
            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
            
            // Read all bytes from the response
            byte[] fileData = response.readAllBytes();
            
            log.info("✅ File downloaded successfully, size: {} bytes", fileData.length);
            return fileData;
            
        } catch (NoSuchKeyException e) {
            log.error("❌ File not found in storage: {}", e.getMessage());
            return null;
        } catch (S3Exception e) {
            log.error("❌ S3 error downloading file: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            log.error("   Error Message: {}", e.awsErrorDetails().errorMessage());
            throw new RuntimeException("Failed to download file from storage: " + e.getMessage(), e);
        } catch (IOException e) {
            log.error("❌ IO Error downloading file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read file data: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error downloading file: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error downloading file: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a file exists in Supabase Storage with logging
     */
    public boolean fileExists(String fileUrl) {
        if (!isConfigured()) {
            log.warn("Supabase storage is not configured; file existence checks are skipped.");
            return false;
        }

        if (fileUrl == null || fileUrl.isEmpty()) {
            log.debug("File URL is null or empty");
            return false;
        }
        
        try {
            String filePath = extractFilePath(fileUrl);
            log.debug("Checking file existence: {}", filePath);
            
            if (filePath == null || filePath.isEmpty()) {
                log.warn("Empty file path extracted from URL: {}", fileUrl);
                return false;
            }
            
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            HeadObjectResponse response = s3Client.headObject(headRequest);
            log.debug("✅ File exists: {}, size: {} bytes", filePath, response.contentLength());
            return true;
            
        } catch (NoSuchKeyException e) {
            log.debug("❌ File does not exist: {}", fileUrl);
            return false;
        } catch (S3Exception e) {
            log.error("❌ Failed to check file existence: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected error checking file existence: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Extract file path from URL - Enhanced version
     */
    private String extractFilePath(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return null;
        }
        
        log.debug("Extracting path from URL: {}", fileUrl);
        
        // Remove query parameters if any
        int questionMarkIndex = fileUrl.indexOf('?');
        if (questionMarkIndex != -1) {
            fileUrl = fileUrl.substring(0, questionMarkIndex);
        }
        
        // Pattern 1: /object/public/bucket-name/path/to/file
        String prefix1 = "/object/public/" + bucketName + "/";
        int startIndex1 = fileUrl.indexOf(prefix1);
        if (startIndex1 != -1) {
            String path = fileUrl.substring(startIndex1 + prefix1.length());
            log.debug("Extracted path using pattern 1: {}", path);
            return path;
        }
        
        // Pattern 2: /storage/v1/object/public/bucket-name/path/to/file
        String prefix2 = "/storage/v1/object/public/" + bucketName + "/";
        int startIndex2 = fileUrl.indexOf(prefix2);
        if (startIndex2 != -1) {
            String path = fileUrl.substring(startIndex2 + prefix2.length());
            log.debug("Extracted path using pattern 2: {}", path);
            return path;
        }
        
        // Pattern 3: Just the path after bucket name
        try {
            java.net.URL url = new java.net.URL(fileUrl);
            String path = url.getPath();
            log.debug("URL path: {}", path);
            
            // Try to find bucket name in path
            String[] pathSegments = path.split("/");
            for (int i = 0; i < pathSegments.length; i++) {
                if (pathSegments[i].equals(bucketName)) {
                    // Build path from after bucket name
                    StringBuilder sb = new StringBuilder();
                    for (int j = i + 1; j < pathSegments.length; j++) {
                        if (sb.length() > 0) {
                            sb.append("/");
                        }
                        sb.append(pathSegments[j]);
                    }
                    String result = sb.toString();
                    log.debug("Extracted path from URL segments: {}", result);
                    return result;
                }
            }
            
            // If we can't find bucket name, use the full path without leading slash
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            log.debug("Using full path: {}", path);
            return path;
            
        } catch (Exception e) {
            log.error("Error parsing URL: {}", e.getMessage());
            return fileUrl; // Return as is as fallback
        }
    }

    /**
     * Generate a signed URL for private files with logging
     */
    public String generateSignedUrl(String fileUrl) {
        if (!isConfigured()) {
            throw new IllegalStateException("Supabase storage is not configured. Set SUPABASE_S3_ENDPOINT, SUPABASE_ACCESS_KEY, SUPABASE_SECRET_KEY, and SUPABASE_BUCKET to generate signed URLs.");
        }

        try {
            String filePath = extractFilePath(fileUrl);
            log.debug("Generating signed URL for: {}", filePath);
            
            if (filePath == null || filePath.isEmpty()) {
                log.error("❌ Invalid file path for URL: {}", fileUrl);
                return null;
            }
            
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1))
                    .getObjectRequest(getObjectRequest)
                    .build();
            
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String signedUrl = presignedRequest.url().toString();
            
            log.debug("✅ Signed URL generated: {}", signedUrl);
            return signedUrl;
            
        } catch (S3Exception e) {
            log.error("❌ Failed to generate signed URL: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            throw new RuntimeException("Failed to generate signed URL: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error generating signed URL: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate signed URL: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from Supabase Storage with logging
     */
    public void deleteFile(String fileUrl) {
        try {
            String filePath = extractFilePath(fileUrl);
            log.info("Deleting file: {}", filePath);
            
            if (filePath == null || filePath.isEmpty()) {
                log.error("❌ Invalid file path for URL: {}", fileUrl);
                return;
            }
            
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            
            DeleteObjectResponse response = s3Client.deleteObject(deleteRequest);
            log.info("✅ File deleted: {}", filePath);
            log.info("   Delete marker: {}", response.deleteMarker());
            
        } catch (S3Exception e) {
            log.error("❌ Failed to delete file: {}", e.getMessage(), e);
            log.error("   Status Code: {}", e.statusCode());
            log.error("   Error Code: {}", e.awsErrorDetails().errorCode());
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error deleting file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "pdf";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Get bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Get Supabase URL
     */
    public String getSupabaseUrl() {
        return supabaseUrl;
    }
}
