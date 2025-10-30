package com.example.file_management_system.model;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "file_metadata")
public class FileMetadata {

    @Id
    private String id;

    private String userId;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String s3Path;
    private Instant uploadedAt;
    private String extractedText;







}
