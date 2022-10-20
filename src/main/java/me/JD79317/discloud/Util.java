package me.JD79317.discloud;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.ArrayList;

public final class Util {
    private Util() {
    }

    public static void reportError(Throwable throwable, String humanMessage) {
        JOptionPane.showMessageDialog(null, humanMessage + "\n" + throwable.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        throwable.printStackTrace();
    }

    public static Cipher getCipher(Key key, @Nullable byte[] ivParameter) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        if (ivParameter != null) {
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivParameter));
        } else {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        }
        return cipher;
    }

    public static void displayTray(String title, String description) {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(Main.ICON, title);
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip(description);
                tray.add(trayIcon);
                trayIcon.displayMessage(title, description, TrayIcon.MessageType.INFO);
            } catch (AWTException ignored) {
            }
        }
    }

    public static String getRelativePath(File inside, File original) {
        if (original.isDirectory()) {
            return original.toPath().relativize(inside.toPath()).toString();
        }
        return inside.getName();
    }

    public static File saveFile(String fileName, Frame frame) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Specify where to save file");
        fileChooser.setSelectedFile(new File(fileName + Main.FILE_SUFFIX));
        int result = fileChooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    public static void resolveFiles(File dir, ArrayList<File> fileList) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File listedFile : files) {
                fileList.add(listedFile);
                if (listedFile.isDirectory()) {
                    resolveFiles(listedFile, fileList);
                }
            }
        }
    }
}
