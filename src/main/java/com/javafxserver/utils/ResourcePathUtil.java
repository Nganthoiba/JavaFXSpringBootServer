package com.javafxserver.utils;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

public class ResourcePathUtil {
	public static Path getAppResourcePath(String resourceFolder) throws URISyntaxException {        
        
     // Determine the path of the running JAR or class file
        File codeSource = new File(ResourcePathUtil.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
        // Navigate to the parent directory (installation root)
        Path installDir = codeSource.getParentFile().toPath();

        // Construct the path to the desired resource folder
        return installDir.resolve(resourceFolder);
    }
}
