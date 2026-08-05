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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

public class StorageServer {

    static String dataDir = "/data";
    static HashMap<String, ReentrantLock> locks = new HashMap<>();

    static synchronized ReentrantLock getLock(String filename) {
        if (!locks.containsKey(filename)) {
            locks.put(filename, new ReentrantLock());
        }
        return locks.get(filename);
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
        new File(dataDir).mkdirs();

        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        server.createContext("/health", ex -> {
            try {
                send(ex, 200, "OK".getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        server.createContext("/upload", ex -> {
            String filename = null;
            try {
                filename = getParam(ex.getRequestURI().getQuery(), "filename");
                byte[] data = ex.getRequestBody().readAllBytes();
                
                ReentrantLock lock = getLock(filename);
                lock.lock();
                try {
                    FileOutputStream out = new FileOutputStream(dataDir + "/" + filename);
                    out.write(data);
                    out.close();
                    System.out.println("Saved: " + filename + " (" + data.length + " bytes)");
                } finally {
                    lock.unlock();
                }
                
                send(ex, 200, "Saved".getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                try { send(ex, 500, "ERROR".getBytes()); } catch (Exception ignore) {}
            }
        });

        server.createContext("/download", ex -> {
            String filename = null;
            try {
                filename = getParam(ex.getRequestURI().getQuery(), "filename");
                File f = new File(dataDir + "/" + filename);
                if (!f.exists()) {
                    send(ex, 404, "Not found".getBytes());
                    return;
                }
                
                ReentrantLock lock = getLock(filename);
                lock.lock();
                try {
                    FileInputStream in = new FileInputStream(f);
                    byte[] data = in.readAllBytes();
                    in.close();
                    send(ex, 200, data);
                } finally {
                    lock.unlock();
                }
            } catch (Exception e) {
                e.printStackTrace();
                try { send(ex, 500, "ERROR".getBytes()); } catch (Exception ignore) {}
            }
        });

        server.createContext("/delete", ex -> {
            String filename = null;
            try {
                filename = getParam(ex.getRequestURI().getQuery(), "filename");
                File f = new File(dataDir + "/" + filename);
                
                ReentrantLock lock = getLock(filename);
                lock.lock();
                try {
                    if (f.exists()) {
                        f.delete();
                        System.out.println("Deleted: " + filename);
                    }
                } finally {
                    lock.unlock();
                }
                
                send(ex, 200, "Deleted".getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                try { send(ex, 500, "ERROR".getBytes()); } catch (Exception ignore) {}
            }
        });

        server.createContext("/list", ex -> {
            try {
                File dir = new File(dataDir);
                File[] files = dir.listFiles();
                StringBuilder sb = new StringBuilder();
                if (files != null) {
                    for (File f : files) {
                        sb.append(f.getName()).append("\n");
                    }
                }
                send(ex, 200, sb.toString().getBytes());
            } catch (Exception e) {
                e.printStackTrace();
                try { send(ex, 500, "ERROR".getBytes()); } catch (Exception ignore) {}
            }
        });

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        server.start();
        System.out.println("Storage server started on port 8081, storing files in " + dataDir);
    }
}