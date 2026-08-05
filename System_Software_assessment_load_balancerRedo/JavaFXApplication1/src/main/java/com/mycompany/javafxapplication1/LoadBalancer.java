/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LoadBalancer {

    static String[] nodes = {"http://lbc_storage_01:8081", "http://lbc_storage_02:8081",
                             "http://lbc_storage_03:8081", "http://lbc_storage_04:8081"};
    static boolean[] healthy = {true, true, true, true};
    static int[] loadCount = {0, 0, 0, 0};

    static String algorithm = "ROUND_ROBIN";
    static int roundRobinIndex = 0;

    static int totalRequests = 0;
    static ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

    static HttpClient client = HttpClient.newHttpClient();

    static synchronized int pickNode() {
        if (algorithm.equals("ROUND_ROBIN")) {
            for (int i = 0; i < nodes.length; i++) {
                int idx = roundRobinIndex % nodes.length;
                roundRobinIndex++;
                if (healthy[idx]) {
                    return idx;
                }
            }
        } else if (algorithm.equals("SJN")) {
            int best = -1;
            for (int i = 0; i < nodes.length; i++) {
                if (healthy[i]) {
                    if (best == -1 || loadCount[i] < loadCount[best]) {
                        best = i;
                    }
                }
            }
            return best;
        } else {
            for (int i = 0; i < nodes.length; i++) {
                if (healthy[i]) {
                    return i;
                }
            }
        }
        return -1;
    }

    static void artificialDelay() {
        try {
            System.out.println("Processing request...");
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static String getParam(String query, String name) {
        if (query == null) {
            return null;
        }
        String[] parts = query.split("&");
        for (String p : parts) {
            String[] kv = p.split("=");
            if (kv.length == 2 && kv[0].equals(name)) {
                return kv[1];
            }
        }
        return null;
    }

    static void send(HttpExchange ex, int code, byte[] body) throws Exception {
        ex.sendResponseHeaders(code, body.length);
        OutputStream os = ex.getResponseBody();
        os.write(body);
        os.close();
    }

    public static void main(String[] args) throws Exception {
        startHealthChecker();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/upload", ex -> {
            try {
                totalRequests++;
                String filename = getParam(ex.getRequestURI().getQuery(), "filename");
                queue.add("upload:" + filename);
                byte[] data = ex.getRequestBody().readAllBytes();
                artificialDelay();
                int node = pickNode();
                if (node == -1) {
                    send(ex, 503, "No healthy nodes".getBytes());
                    return;
                }
                loadCount[node]++;
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(nodes[node] + "/upload?filename=" + filename))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                        .build();
                HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                queue.remove("upload:" + filename);
                send(ex, 200, resp.body());
            } catch (Exception e) {
                e.printStackTrace();
                try { send(ex, 500, "ERROR".getBytes()); } catch (Exception ignore) {}
            }
        });

        server.createContext("/download", ex -> {
            try {
                totalRequests++;
                String filename = getParam(ex.getRequestURI().getQuery(), "filename");
                artificialDelay();
                int node = pickNode();
                if (node == -1) {
                    send(ex, 503, "No healthy nodes".getBytes());
                    return;
                }
                loadCount[node]++;
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(nodes[node] + "/download?filename=" + filename))
                        .GET()
                        .build();
                HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                send(ex, resp.statusCode(), resp.body());
            } catch (Exception e) {
                e.printStackTrace();
                try { send(ex, 500, "ERROR".getBytes()); } catch (Exception ignore) {}
            }
        });

        server.createContext("/delete", ex -> {
            try {
                totalRequests++;
                String filename = getParam(ex.getRequestURI().getQuery(), "filename");
                int node = pickNode();
                if (node == -1) {
                    send(ex, 503, "No healthy nodes".getBytes());
                    return;
                }
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(nodes[node] + "/delete?filename=" + filename))
                        .DELETE()
                        .build();
                HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
                send(ex, 200, resp.body());
            } catch (Exception e) {
                e.printStackTrace();
                try { send(ex, 500, "ERROR".getBytes()); } catch (Exception ignore) {}
            }
        });

        server.createContext("/health", ex -> {
            try {
                send(ex, 200, "OK".getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.createContext("/metrics", ex -> {
            try {
                int healthyCount = 0;
                for (boolean h : healthy) {
                    if (h) healthyCount++;
                }
                String out = "total_requests=" + totalRequests + "\n"
                        + "queue_size=" + queue.size() + "\n"
                        + "healthy_nodes=" + healthyCount + "\n"
                        + "algorithm=" + algorithm + "\n";
                send(ex, 200, out.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.createContext("/algorithm", ex -> {
            try {
                String name = getParam(ex.getRequestURI().getQuery(), "name");
                if (name != null) {
                    algorithm = name;
                    System.out.println("Algorithm switched to " + algorithm);
                }
                send(ex, 200, ("algorithm=" + algorithm).getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Load balancer started on port 8080");
    }

    static void startHealthChecker() {
        Thread t = new Thread(() -> {
            while (true) {
                for (int i = 0; i < nodes.length; i++) {
                    try {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(nodes[i] + "/health"))
                                .GET()
                                .build();
                        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                        healthy[i] = resp.statusCode() == 200;
                    } catch (Exception e) {
                        healthy[i] = false;
                    }
                }
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }
}
