package com.lostfound.service;

import com.lostfound.exception.StorageException;
import com.lostfound.util.ConfigLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * =====================================================================
 * CLOUD SERVICE SDK BOUNDARY
 * =====================================================================
 * This class is the ONLY place in the entire application that talks
 * to an external cloud API. Everything else — GUI, DAO, RMI services,
 * business logic — is strict Core Java with zero third-party
 * dependencies. This class itself is ALSO pure Core Java: it uses
 * java.net.http.HttpClient (standard since JDK 11) to call
 * Cloudinary's unsigned-upload REST endpoint directly, rather than
 * pulling in Cloudinary's Java SDK as a dependency. That keeps the
 * "Core Java only" requirement intact even at the cloud-storage
 * boundary — there is no framework here, just an HTTP POST.
 * =====================================================================
 *
 * Configure via resources/config.properties:
 *   storage.provider    = CLOUDINARY
 *   storage.bucket.url  = <your Cloudinary cloud name>
 *   storage.access.key  = <your UNSIGNED upload preset name>
 *   storage.secret.key  = (unused for unsigned uploads; kept for a
 *                          future signed-upload variant)
 *
 * To use a different provider (AWS S3, Supabase Storage, etc.),
 * this is the only class that needs to change — everything else
 * depends on the StorageService interface, not this implementation.
 */
public class CloudStorageService implements StorageService {

    private static final Pattern SECURE_URL_PATTERN =
            Pattern.compile("\"secure_url\"\\s*:\\s*\"([^\"]+)\"");

    private final String cloudName;
    private final String uploadPreset;
    private final HttpClient httpClient;

    public CloudStorageService() {
        this.cloudName = ConfigLoader.get("storage.bucket.url");
        this.uploadPreset = ConfigLoader.get("storage.access.key");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String uploadImage(File file, String desiredFileName) throws StorageException {
        if (file == null || !file.exists()) {
            throw new StorageException("Source image file does not exist.");
        }

        String boundary = "----LostFoundBoundary" + UUID.randomUUID();
        String uploadUrl = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";

        try {
            byte[] body = buildMultipartBody(file, desiredFileName, boundary);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new StorageException("Cloud storage upload failed (HTTP " +
                        response.statusCode() + "): " + response.body());
            }

            Matcher matcher = SECURE_URL_PATTERN.matcher(response.body());
            if (!matcher.find()) {
                throw new StorageException("Cloud storage upload succeeded but no secure_url was found in the response.");
            }
            return matcher.group(1);

        } catch (IOException e) {
            throw new StorageException("Failed to upload image to cloud storage: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException("Image upload was interrupted.", e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload"))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() < 500;
        } catch (Exception e) {
            return false;
        }
    }

    /** Hand-builds a multipart/form-data body — no library needed for this either. */
    private byte[] buildMultipartBody(File file, String desiredFileName, String boundary) throws IOException {
        String fileName = desiredFileName != null ? desiredFileName : file.getName();
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeField(out, boundary, "upload_preset", uploadPreset);

        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n")
                .getBytes(StandardCharsets.UTF_8));
        out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(fileBytes);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return out.toByteArray();
    }

    private void writeField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }
}
