package com.example.file_management_system.services;

import com.example.file_management_system.model.FileMetadata;
import com.example.file_management_system.respository.FileMetadataRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FileUploader {

    private static final Logger log = LoggerFactory.getLogger(FileUploader.class);

    private final S3Client s3Client;
    private final FileMetadataRepository repo;

    @Value("${app.s3.bucket}")
    private String bucket;

    public FileUploader(S3Client s3Client, FileMetadataRepository repo) {
        this.s3Client = s3Client;
        this.repo = repo;
    }

    public FileMetadata uploadFile(String userId, MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) originalFilename = "unnamed";

        String storedFileName = UUID.randomUUID() + "_" + originalFilename;
        String key = userId + "/" + storedFileName;


        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize()));


        FileMetadata metadata = new FileMetadata();
        metadata.setUserId(userId);
        metadata.setFileName(originalFilename);
        metadata.setFileType(file.getContentType());
        metadata.setFileSize(file.getSize());
        metadata.setS3Path(key);
        metadata.setUploadedAt(Instant.now());


        if (isPdf(originalFilename, file.getContentType())) {
            try (InputStream in = file.getInputStream()) {
                String text = extractTextFromPdf(in);
                metadata.setExtractedText(text);
            } catch (Exception e) {
                log.warn("Failed to extract text from PDF: {}", originalFilename, e);
            }
        }

        return repo.save(metadata);
    }

    public List<FileMetadata> listFiles(String userId) {
        return repo.findByUserId(userId);
    }

    public FileMetadata getMetadataById(String id) {
        return repo.findById(id).orElse(null);
    }

    public FileMetadata getMetadataByUserAndName(String userId, String fileName) {
        return repo.findByUserIdAndFileName(userId, fileName);
    }

    public byte[] downloadFileByS3Path(String s3Path) {
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Path)
                .build();

        return s3Client.getObjectAsBytes(getReq).asByteArray();
    }

    private String extractTextFromPdf(InputStream pdfInputStream) {
        try {
            byte[] pdfBytes = pdfInputStream.readAllBytes(); // Java 9+
            try (PDDocument document = Loader.loadPDF(pdfBytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } catch (IOException e) {
            log.error("Error extracting text from PDF", e);
            return null;
        }
    }

    private boolean isPdf(String filename, String contentType) {
        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) return true;
        return filename.toLowerCase().endsWith(".pdf");
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }
        return bos.toByteArray();
    }
}
