package me.JD79317.discloud;

import me.JD79317.discloud.frames.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    public static final Image ICON = Toolkit.getDefaultToolkit().getImage(Main.class.getResource("/icon.png"));
    public static final String FILE_SUFFIX = ".discloud";

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new MainFrame();
        frame.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            EXECUTOR.shutdownNow();
            clearTmpDirectory();
        }));
    }

    public static void clearTmpDirectory() {
        try {
            File[] files = FileSplitter.TMP_FOLDER.listFiles();
            if (files != null) {
                for (File file : files) {
                    Files.delete(file.toPath());
                }
            }
            Files.delete(FileSplitter.TMP_FOLDER.toPath());
        } catch (IOException ignored) {
        }
    }
}
