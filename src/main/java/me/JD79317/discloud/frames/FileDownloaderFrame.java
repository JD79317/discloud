package me.JD79317.discloud.frames;

import me.JD79317.discloud.DiscloudData;
import me.JD79317.discloud.Main;
import org.json.simple.parser.ParseException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

import static me.JD79317.discloud.Util.*;

public final class FileDownloaderFrame extends JFrame {
    private final File selectedFile;
    private final transient DiscloudData data;
    private final transient Cipher cipher;
    private JList<String> list;
    private JProgressBar progressBar;

    public FileDownloaderFrame(File selectedFile) throws IOException, ParseException, GeneralSecurityException {
        this.data = DiscloudData.deserialize(Files.readString(selectedFile.toPath()));
        this.selectedFile = selectedFile;
        byte[] decodedKey = Base64.getDecoder().decode(data.getKey());
        SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
        this.cipher = getCipher(secretKey, Base64.getDecoder().decode(data.getIv()));

        setTitle("Discloud File Downloader");
        setSize(400, 300);
        setLayout(null);
        setResizable(false);
        setIconImage(Main.ICON);
        initComponent();
        setLocationRelativeTo(null);
    }

    private void initComponent() {
        list = new JList<>();
        DefaultListModel<String> model = new DefaultListModel<>();
        model.addAll(data.getUrlPath().keySet());
        list.setModel(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBackground(Color.gray);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);

        JScrollPane scrollPane = new JScrollPane(list);

        final JButton getButton = new JButton("Download Selected");
        getButton.addActionListener(event -> {
            List<String> files = new ArrayList<>();

            DefaultListModel<String> listModel = (DefaultListModel<String>) list.getModel();
            if (list.getSelectedIndices().length > 0) {
                int[] selectedIndices = list.getSelectedIndices();
                for (int i = selectedIndices.length - 1; i >= 0; i--) {
                    String elem = list.getModel().getElementAt(selectedIndices[i]);
                    listModel.removeElementAt(selectedIndices[i]);
                    files.add(elem);
                }
            }
            if (!files.isEmpty()) {
                downloadFiles(files);
            }
        });

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        bottomPanel.add(getButton, BorderLayout.SOUTH);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void downloadFiles(List<String> files) {
        Main.EXECUTOR.execute(() -> {
            int chunksRequired = 0;
            for (String file : files) {
                chunksRequired += data.getUrlPath().get(file).size();
            }
            progressBar.setMaximum(chunksRequired);
            progressBar.setValue(0);
            for (String file : files) {
                System.out.println("Downloading " + file + "...");
                download(file);
            }
            displayTray("Download Complete", "Download of " + files.size() + " files have completed.");
        });
    }

    private void download(String fileName) {
        List<String> urls = data.getUrlPath().get(fileName);
        File file = new File(selectedFile.getParentFile(), (data.getFolderName() != null ? data.getFolderName() + File.separatorChar : "") + fileName);
        file.getParentFile().mkdirs();
        try (FileOutputStream fileOut = new FileOutputStream(file)) {
            long currentPosition = 0;
            FileChannel outChannel = fileOut.getChannel();

            urls.sort(new Comparator<String>() {
                public int compare(String o1, String o2) {
                    return extractInt(o1) - extractInt(o2);
                }

                int extractInt(String string) {
                    String chunkNumber = string.substring(string.lastIndexOf("/") + 1, string.indexOf("_"));
                    return Integer.parseInt(chunkNumber);
                }
            });

            for (String url : urls) {
                try (ReadableByteChannel readableByteChannel = Channels.newChannel(new CipherInputStream(new URL(url).openStream(), cipher))) {
                    currentPosition += outChannel.transferFrom(readableByteChannel, currentPosition, Long.MAX_VALUE);
                }
                progressBar.setValue(progressBar.getValue() + 1);
            }
        } catch (IOException exception) {
            reportError(exception, "Failed to download files from discord cdn. (Does the channel still exist?)");
        }
    }
}
