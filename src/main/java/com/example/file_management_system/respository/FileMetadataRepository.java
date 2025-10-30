package com.example.file_management_system.respository;

import com.example.file_management_system.model.FileMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FileMetadataRepository extends MongoRepository<FileMetadata, String> {

    List<FileMetadata> findByUserId(String userId);
    FileMetadata findByUserIdAndFileName(String userId, String fileName);
}
