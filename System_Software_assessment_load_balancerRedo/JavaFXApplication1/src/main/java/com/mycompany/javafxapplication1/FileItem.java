/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.javafxapplication1;

/**
 *
 * @author ntu-user
 */
public class FileItem {
    private String filename;
    private String owner;
    private long size;
    private int chunks;

    public FileItem(String filename, String owner, long size, int chunks) {
        this.filename = filename;
        this.owner = owner;
        this.size = size;
        this.chunks = chunks;
    }

    public String getFilename() { return filename; }
    public String getOwner() { return owner; }
    public long getSize() { return size; }
    public int getChunks() { return chunks; }
}
