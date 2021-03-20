package de.penguins.pingubot.plugins.impl.roulette;

import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class BoardImageGenerator {
    private static final String FILE_PATH = "roulette/roulette_board_small.png";
    private static final double FILE_SCALE = 0.5;
    private static final int FIELD_OFFSET = (int) (10 * FILE_SCALE);

    public static byte[] getImageFile(RouletteBoard board) {
        try {
            BufferedImage img = ImageIO.read(new ClassPathResource(FILE_PATH).getInputStream());
            Graphics graphics = img.getGraphics();

            for (RouletteField field : board.getFields()) {
                field.drawPlayerPieces(graphics, board.getPlayers(), FILE_SCALE, FIELD_OFFSET);
            }
            graphics.dispose();

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ImageIO.write(img, "png", byteStream);

            return byteStream.toByteArray();
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        return null;
    }

    public static String getImageFileName() {
        return Path.of(FILE_PATH).getFileName().toString();
    }
}
