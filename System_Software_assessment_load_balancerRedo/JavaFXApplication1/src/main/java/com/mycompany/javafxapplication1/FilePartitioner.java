/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class FilePartitioner {

    static final int CHUNK_SIZE = 512 * 1024;
    // FIX: app runs inside ntu-vm-comp20081 container - use Docker hostname, not localhost
    static String loadBalancer = "http://lbc_load_balancer:8080";

    HttpClient client = HttpClient.newHttpClient();

    public int uploadFile(File file, String owner) throws Exception {
        byte[] raw = Files.readAllBytes(file.toPath());
        byte[] encrypted = AESUtil.encrypt(raw);
        int chunks = (int) Math.ceil((double) encrypted.length / CHUNK_SIZE);
        System.out.println("Uploading " + file.getName() + " in " + chunks + " chunk(s)");
        for (int i = 0; i < chunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, encrypted.length);
            byte[] chunk = Arrays.copyOfRange(encrypted, start, end);
            String chunkName = file.getName() + ".part" + i;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(loadBalancer + "/upload?filename=" + chunkName))
                    .POST(HttpRequest.BodyPublishers.ofByteArray(chunk))
                    .build();
            client.send(req, HttpResponse.BodyHandlers.ofString());
        }
        return chunks;
    }

    public byte[] downloadFile(String filename, int chunks) throws Exception {
        ByteArrayOutputStream all = new ByteArrayOutputStream();
        for (int i = 0; i < chunks; i++) {
            String chunkName = filename + ".part" + i;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(loadBalancer + "/download?filename=" + chunkName))
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            all.write(resp.body());
        }
        byte[] decrypted = AESUtil.decrypt(all.toByteArray());
        return decrypted;
    }

    public void saveTo(byte[] data, File dest) throws Exception {
        FileOutputStream out = new FileOutputStream(dest);
        out.write(data);
        out.close();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("FilePartitioner service running...");
        Thread.sleep(Long.MAX_VALUE);
    }
}
