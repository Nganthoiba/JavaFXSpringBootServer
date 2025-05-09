package com.javafxserver.digitalsigner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.ClassPathResource;

public class FontLoader {
    public static File getFontFile(String fontName) throws IOException {
        ClassPathResource resource = new ClassPathResource("fonts/" + fontName);
        if (!resource.exists()) {
            throw new IOException("Font file not found: " + resource.getPath());
        }

        // Convert to temporary file since ClassPathResource does not provide a direct File reference
        Path tempFile = Files.createTempFile("font_", ".ttf");
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile.toFile();
    }
    
    public static boolean isFontAvailable(String fontName) {
        ClassPathResource resource = new ClassPathResource("fonts/" + fontName);
        String pathString = resource.getPath();
        System.out.println("Font path: " + pathString);
        return resource.exists();
    }
    
}
