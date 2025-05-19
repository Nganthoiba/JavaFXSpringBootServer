package com.javafxserver.utils;

import java.io.File;
import java.io.IOException;
import com.javafxserver.config.Config;

import org.springframework.web.multipart.MultipartFile;
public class ConvertFile {
	public static File toFile(MultipartFile multipartFile) throws IOException {
		try {
			String originalFileName = multipartFile.getOriginalFilename();
		    //File convFile = new File(multipartFile.getOriginalFilename());
			File convFile = File.createTempFile(Config.STORAGE_PATH + File.separator + originalFileName, ".pdf");
			
			if (!convFile.exists()) {
			    convFile.createNewFile();
			}

			multipartFile.transferTo(convFile);
		    return convFile;
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
			throw new IOException("Error in conversion from multipart file to file. "+ioe.getMessage(), ioe);
		}
	}
}
