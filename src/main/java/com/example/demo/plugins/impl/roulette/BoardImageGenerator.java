package com.example.demo.plugins.impl.roulette;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public class BoardImageGenerator {
    private static final String FILE_PATH = "roulette_board.png";

    public static File getImageFile() {
        try {
            return new ClassPathResource(FILE_PATH).getFile();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        return null;
    }
}
