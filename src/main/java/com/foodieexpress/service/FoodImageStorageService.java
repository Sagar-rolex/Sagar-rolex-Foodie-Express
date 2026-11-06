package com.foodieexpress.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;

@Service
public class FoodImageStorageService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final Path uploadDirectory;
    private final Path restaurantBannerDirectory;

    public FoodImageStorageService(@Value("${food.image.upload-dir:uploads/foods}") String directory) {
        this(directory, "uploads/restaurants");
    }

    @org.springframework.beans.factory.annotation.Autowired
    public FoodImageStorageService(@Value("${food.image.upload-dir:uploads/foods}") String directory,
            @Value("${restaurant.banner.upload-dir:uploads/restaurants}") String bannerDirectory) {
        this.uploadDirectory = Path.of(directory).toAbsolutePath().normalize();
        this.restaurantBannerDirectory = Path.of(bannerDirectory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        return store(file, uploadDirectory, "/uploads/foods/");
    }

    public String storeRestaurantBanner(MultipartFile file) {
        return store(file, restaurantBannerDirectory, "/uploads/restaurants/");
    }

    private String store(MultipartFile file, Path directory, String urlPrefix) {
        if (file == null || file.isEmpty()) throw new RuntimeException("Select an image to upload");
        if (file.getSize() > MAX_BYTES) throw new RuntimeException("Image must be 5 MB or smaller");
        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException exception) { throw new RuntimeException("Unable to read uploaded image"); }
        String extension = imageExtension(bytes);
        String contentType = file.getContentType();
        if (extension == null || !matchesContentType(extension, contentType)) {
            throw new RuntimeException("Only valid JPEG, PNG, and WebP images are accepted");
        }
        String filename = UUID.randomUUID() + extension;
        Path destination = directory.resolve(filename).normalize();
        if (!destination.getParent().equals(directory)) throw new RuntimeException("Invalid upload path");
        try {
            Files.createDirectories(directory);
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new RuntimeException("Unable to store uploaded image");
        }
        return urlPrefix + filename;
    }

    public Path getUploadDirectory() { return uploadDirectory; }
    public Path getRestaurantBannerDirectory() { return restaurantBannerDirectory; }

    private String imageExtension(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff) return ".jpg";
        if (b.length >= 8 && (b[0] & 0xff) == 0x89 && b[1] == 0x50 && b[2] == 0x4e && b[3] == 0x47 && b[4] == 0x0d && b[5] == 0x0a && b[6] == 0x1a && b[7] == 0x0a) return ".png";
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return ".webp";
        return null;
    }

    private boolean matchesContentType(String ext, String type) {
        if (type == null) return false;
        String normalized = type.toLowerCase(Locale.ROOT);
        return switch (ext) {
            case ".jpg" -> normalized.equals("image/jpeg") || normalized.equals("image/jpg");
            case ".png" -> normalized.equals("image/png");
            case ".webp" -> normalized.equals("image/webp");
            default -> false;
        };
    }
}
