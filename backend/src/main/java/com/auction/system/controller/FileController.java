package com.auction.system.controller;

import com.auction.system.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            logger.info("Uploading file: {}", file.getOriginalFilename());
            
            String fileName = fileStorageService.storeFile(file);
            // Use /uploads/ path so Nginx serves the file directly
            String fileUrl = "/uploads/" + fileName;

            Map<String, Object> response = new HashMap<>();
            response.put("fileName", fileName);
            response.put("fileUrl", fileUrl);
            response.put("fileSize", file.getSize());
            response.put("success", true);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to upload file", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String fileName) {
        try {
            Path filePath = fileStorageService.getFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if(resource.exists() && resource.isReadable()) {
                // Try to determine file's content type
                String contentType = "application/octet-stream";
                
                if(fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if(fileName.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if(fileName.toLowerCase().endsWith(".gif")) {
                    contentType = "image/gif";
                } else if(fileName.toLowerCase().endsWith(".webp")) {
                    contentType = "image/webp";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to serve file: {}", fileName, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{fileName:.+}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileName) {
        try {
            fileStorageService.deleteFile(fileName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File deleted successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to delete file: {}", fileName, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}


