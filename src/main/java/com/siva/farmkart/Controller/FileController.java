package com.siva.farmkart.Controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class FileController {
    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) throws Exception {
        String path = request.getRequestURI().substring("/uploads/".length());
        Path filePath = Paths.get("uploads/").resolve(path).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists()) {
            String contentType = Files.probeContentType(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .body(resource);
        }
        return ResponseEntity.notFound().build();
    }
}
