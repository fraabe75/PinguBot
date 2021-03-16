package com.example.demo.plugins.impl.roulette;

import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BoardImageGenerator {
    private static final String FILE_PATH = "roulette_board.png";

    public static byte[] getImageFile(RouletteBoard board) {
        try {
            BufferedImage img = ImageIO.read(new ClassPathResource(FILE_PATH).getFile());
            Graphics graphics = img.getGraphics();
            int width = img.getWidth();
            int height = img.getHeight();

            //graphics.setColor();
            //graphics.fillOval(20, 120, 5, 5);
            graphics.dispose();

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ImageIO.write(img, "png", byteStream);

            return byteStream.toByteArray();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        return null;
    }
}
