package com.sunmoon.backend.service;

import com.sunmoon.backend.dto.response.FileObjectInfo;
import io.minio.MinioClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface MinioService {
    String upload(MultipartFile file) throws Exception;
    String upload(String bucketName, MultipartFile file) throws Exception;
    String upload(MultipartFile file, String objectName) throws Exception;
    String upload(String bucketName, MultipartFile file, String objectName) throws Exception;
    
    byte[] download(String objectName) throws Exception;
    byte[] download(String bucketName, String objectName) throws Exception;
    
    boolean delete(String objectName);
    boolean delete(String bucketName, String objectName);
    
    List<String> list(String prefix) throws Exception;
    List<String> list(String bucketName, String prefix) throws Exception;
    
    boolean exists(String objectName);
    boolean exists(String bucketName, String objectName);
    
    String getPublicUrl(String objectName);
    String getPublicUrl(String bucketName, String objectName);
    
    String generatePresignedUploadUrl(String objectName, int expirySeconds) throws Exception;
    String generatePresignedUploadUrl(String bucketName, String objectName, int expirySeconds) throws Exception;
    
    String generatePresignedDownloadUrl(String objectName, int expirySeconds) throws Exception;
    String generatePresignedDownloadUrl(String bucketName, String objectName, int expirySeconds) throws Exception;

    InputStream getObjectRange(String objectName, Long start, Long end) throws Exception;
    InputStream getObjectRange(String bucketName, String objectName, Long start, Long end) throws Exception;
    
    String getBucketName();
    MinioClient getMinioClient();

    FileObjectInfo getObjectInfo(String objectName) throws Exception;
    FileObjectInfo getObjectInfo(String bucketName, String objectName) throws Exception;
}
