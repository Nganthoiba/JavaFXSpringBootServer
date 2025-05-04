package com.javafxserver.utils;


import java.io.File;
import java.io.IOException;

import org.apache.commons.fileupload.FileItem;
public class FileContext {

	private FileItem fileItem;
	
	public FileContext(FileItem item) {
		this.fileItem = item;
	}
	
	public FileItem getFileItem() {
		return this.fileItem;
	}
	
	public String getFileName() {
		// Sanitize the file name to prevent security issues
		return new File(this.fileItem.getName()).getName();
	}
	
	public String saveFile(String uploadFilePath) throws Exception{
        String fileName = this.getFileName(); // Extracts file name
        return saveFile(uploadFilePath, fileName);
	}
	
	public String saveFile(String uploadFilePath, String fileName/** Desired file name if required */) throws Exception{
		// Ensure the upload directory exists
        File uploadDirectory = new File(uploadFilePath);
        if (!uploadDirectory.exists()) {
        	if (!uploadDirectory.mkdirs()) {
                throw new IOException("Failed to create upload directory: " + uploadFilePath);
            }
        }        
        // Create a File object for the destination file
        File uploadedFile = new File(uploadDirectory, fileName);

        // Write the file to disk
        this.fileItem.write(uploadedFile);
        
        return uploadedFile.getAbsolutePath();
	}
	
	public byte[] get() {
		return this.fileItem.get();
	}

}
