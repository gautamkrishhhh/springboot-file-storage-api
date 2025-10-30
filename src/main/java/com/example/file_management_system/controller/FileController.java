package com.example.file_management_system.controller;

import com.example.file_management_system.model.FileMetadata;
import com.example.file_management_system.services.FileUploader;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileUploader fileUploader;

    public FileController(FileUploader fileUploader) {
        this.fileUploader = fileUploader;
    }

    /** ✅ Upload file for a given userId */
    @PostMapping("/upload")
    public ResponseEntity<FileMetadata> uploadFile(@PathVariable String userId,
                                                   @RequestParam("file") MultipartFile file) throws IOException {
        FileMetadata metadata = fileUploader.uploadFile(userId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(metadata);
    }

    /** ✅ List all files uploaded by a user */
    @GetMapping("/{userId}")
    public ResponseEntity<List<FileMetadata>> listFiles(@PathVariable String userId) {
        List<FileMetadata> files = fileUploader.listFiles(userId);
        return ResponseEntity.ok(files);
    }

    /** ✅ Download file by metadata ID */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadFileById(@PathVariable String id) {
        FileMetadata meta = fileUploader.getMetadataById(id);
        if (meta == null) return ResponseEntity.notFound().build();

        byte[] data = fileUploader.downloadFileByS3Path(meta.getS3Path());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(meta.getFileName())
                .build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(data.length);

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /** ✅ Download file by userId and original filename */
    @GetMapping("/download/{userId}/by-name")
    public ResponseEntity<byte[]> downloadByUserAndName(@PathVariable String userId,
                                                        @RequestParam("fileName") String fileName) {
        FileMetadata meta = fileUploader.getMetadataByUserAndName(userId, fileName);
        if (meta == null) return ResponseEntity.notFound().build();

        byte[] data = fileUploader.downloadFileByS3Path(meta.getS3Path());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(meta.getFileName())
                .build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(data.length);

        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    /** ✅ Get extracted text (for PDF files) */
    @GetMapping("/text/{id}")
    public ResponseEntity<String> getExtractedText(@PathVariable String id) {
        FileMetadata meta = fileUploader.getMetadataById(id);
        if (meta == null) return ResponseEntity.notFound().build();
        if (meta.getExtractedText() == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(meta.getExtractedText());
    }
}
