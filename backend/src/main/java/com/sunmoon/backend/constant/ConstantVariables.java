package com.sunmoon.backend.constant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ConstantVariables {

    // ===================== App info =====================
    public static String APP_NAME;
    public static String SERVER_PORT;

    // ===================== OpenAPI =====================
    public static String API_TITLE;
    public static String API_VERSION;
    public static String API_DESCRIPTION;

    // ===================== Database =====================
    public static String DB_URL;
    public static String DB_USERNAME;
    public static String DB_PASSWORD;

    // ===================== JPA / Hibernate =====================
    public static String JPA_DDL_AUTO;
    public static String JPA_DIALECT;

    // ===================== JWT =====================
    public static String SIGNER_KEY;

    // ===================== SpringDoc OpenAPI =====================
    public static String SWAGGER_API_DOCS_PATH;
    public static String SWAGGER_UI_PATH;
    public static String SWAGGER_PACKAGES_TO_SCAN;

    // ===================== Multipart / File storage =====================
    public static String MAX_FILE_SIZE;
    public static String MAX_REQUEST_SIZE;

    // ===================== MinIO =====================
    public static String MINIO_ENDPOINT;
    public static String MINIO_ENDPOINT_UI;
    public static String MINIO_DOMAIN;
    public static String MINIO_ACCESS_KEY;
    public static String MINIO_SECRET_KEY;
    public static String MINIO_BUCKET_NAME;

    // ===================== HikariCP =====================
    public static String HIKARI_MINIMUM_IDLE;
    public static String HIKARI_MAXIMUM_POOL_SIZE;
    public static String HIKARI_POOL_NAME;

    // ===================== Redis =====================
    public static String REDIS_HOST;
    public static int REDIS_PORT;
    public static int REDIS_DATABASE;
    public static String REDIS_PASSWORD;
    public static int REDIS_POOL_MAX_ACTIVE;
    public static int REDIS_POOL_MAX_IDLE;
    public static int REDIS_POOL_MIN_IDLE;
    public static String REDIS_POOL_MAX_WAIT;

    // ========================================================
    // Setters (Spring inject qua @Value)
    // ========================================================

    @Value("${spring.application.name}")
    public void setAppName(String appName) {
        APP_NAME = appName;
    }

    @Value("${server.port}")
    public void setServerPort(String serverPort) {
        SERVER_PORT = serverPort;
    }

    @Value("${open.api.title}")
    public void setApiTitle(String apiTitle) {
        API_TITLE = apiTitle;
    }

    @Value("${open.api.version}")
    public void setApiVersion(String apiVersion) {
        API_VERSION = apiVersion;
    }

    @Value("${open.api.description}")
    public void setApiDescription(String apiDescription) {
        API_DESCRIPTION = apiDescription;
    }

    @Value("${spring.datasource.url}")
    public void setDbUrl(String dbUrl) {
        DB_URL = dbUrl;
    }

    @Value("${spring.datasource.username}")
    public void setDbUsername(String dbUsername) {
        DB_USERNAME = dbUsername;
    }

    @Value("${spring.datasource.password}")
    public void setDbPassword(String dbPassword) {
        DB_PASSWORD = dbPassword;
    }

    @Value("${spring.jpa.hibernate.ddl-auto}")
    public void setJpaDdlAuto(String jpaDdlAuto) {
        JPA_DDL_AUTO = jpaDdlAuto;
    }

    @Value("${spring.jpa.properties.hibernate.dialect}")
    public void setJpaDialect(String jpaDialect) {
        JPA_DIALECT = jpaDialect;
    }

    @Value("${constant.key.signer-key}")
    public void setSignerKey(String signerKey) {
        SIGNER_KEY = signerKey;
    }

    @Value("${springdoc.api-docs.path}")
    public void setSwaggerApiDocsPath(String swaggerApiDocsPath) {
        SWAGGER_API_DOCS_PATH = swaggerApiDocsPath;
    }

    @Value("${springdoc.swagger-ui.path}")
    public void setSwaggerUiPath(String swaggerUiPath) {
        SWAGGER_UI_PATH = swaggerUiPath;
    }

    @Value("${springdoc.packages-to-scan}")
    public void setSwaggerPackagesToScan(String swaggerPackagesToScan) {
        SWAGGER_PACKAGES_TO_SCAN = swaggerPackagesToScan;
    }

    @Value("${spring.servlet.multipart.max-file-size}")
    public void setMaxFileSize(String maxFileSize) {
        MAX_FILE_SIZE = maxFileSize;
    }

    @Value("${spring.servlet.multipart.max-request-size}")
    public void setMaxRequestSize(String maxRequestSize) {
        MAX_REQUEST_SIZE = maxRequestSize;
    }

    @Value("${minio.endpoint}")
    public void setMinioEndpoint(String minioEndpoint) {
        MINIO_ENDPOINT = minioEndpoint;
    }

    @Value("${minio.endpointui}")
    public void setMinioEndpointUi(String minioEndpointUi) {
        MINIO_ENDPOINT_UI = minioEndpointUi;
    }

    @Value("${minio.domain}")
    public void setMinioDomain(String minioDomain) {
        MINIO_DOMAIN = minioDomain;
    }

    @Value("${minio.accessKey}")
    public void setMinioAccessKey(String minioAccessKey) {
        MINIO_ACCESS_KEY = minioAccessKey;
    }

    @Value("${minio.secretKey}")
    public void setMinioSecretKey(String minioSecretKey) {
        MINIO_SECRET_KEY = minioSecretKey;
    }

    @Value("${minio.bucketName}")
    public void setMinioBucketName(String minioBucketName) {
        MINIO_BUCKET_NAME = minioBucketName;
    }

    @Value("${spring.datasource.hikari.minimum-idle}")
    public void setHikariMinimumIdle(String hikariMinimumIdle) {
        HIKARI_MINIMUM_IDLE = hikariMinimumIdle;
    }

    @Value("${spring.datasource.hikari.maximum-pool-size}")
    public void setHikariMaximumPoolSize(String hikariMaximumPoolSize) {
        HIKARI_MAXIMUM_POOL_SIZE = hikariMaximumPoolSize;
    }

    @Value("${spring.datasource.hikari.pool-name}")
    public void setHikariPoolName(String hikariPoolName) {
        HIKARI_POOL_NAME = hikariPoolName;
    }

    @Value("${spring.data.redis.host}")
    public void setRedisHost(String redisHost) {
        REDIS_HOST = redisHost;
    }

    @Value("${spring.data.redis.port}")
    public void setRedisPort(int redisPort) {
        REDIS_PORT = redisPort;
    }

    @Value("${spring.data.redis.database}")
    public void setRedisDatabase(int redisDatabase) {
        REDIS_DATABASE = redisDatabase;
    }

    @Value("${spring.data.redis.password}")
    public void setRedisPassword(String redisPassword) {
        REDIS_PASSWORD = redisPassword;
    }

    @Value("${spring.data.redis.lettuce.pool.max-active}")
    public void setRedisPoolMaxActive(int redisPoolMaxActive) {
        REDIS_POOL_MAX_ACTIVE = redisPoolMaxActive;
    }

    @Value("${spring.data.redis.lettuce.pool.max-idle}")
    public void setRedisPoolMaxIdle(int redisPoolMaxIdle) {
        REDIS_POOL_MAX_IDLE = redisPoolMaxIdle;
    }

    @Value("${spring.data.redis.lettuce.pool.min-idle}")
    public void setRedisPoolMinIdle(int redisPoolMinIdle) {
        REDIS_POOL_MIN_IDLE = redisPoolMinIdle;
    }

    @Value("${spring.data.redis.lettuce.pool.max-wait}")
    public void setRedisPoolMaxWait(String redisPoolMaxWait) {
        REDIS_POOL_MAX_WAIT = redisPoolMaxWait;
    }
}