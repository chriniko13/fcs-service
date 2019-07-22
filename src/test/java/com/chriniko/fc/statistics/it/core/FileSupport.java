package com.chriniko.fc.statistics.it.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileSupport {

    public static String read(String resourceName) {
        try {
            URL url = FileSupport.class.getClassLoader().getResource(resourceName);
            Path path = Paths.get(url.toURI());
            return String.join("", Files.readAllLines(path));
        } catch (URISyntaxException | IOException e) {
            throw new TestInfraException("could not read resource: " + resourceName, e);
        }
    }
}
