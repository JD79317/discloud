package me.JD79317.discloud.frames;

import me.JD79317.discloud.FileUploader;
import me.JD79317.discloud.Main;
import me.JD79317.discloud.Util;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public final class MainFrame extends JFrame {
    private final JButton uploadButton = new JButton("Upload Files To CDN");
    private final JButton downloadButton = new JButton("Download Files From CDN");
    private final JLabel authorCredit = new JLabel("Made by JD79317");
    private final JTextPane dragAndDropPanel = new JTextPane();


    public MainFrame() {
        setTitle("Discloud");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 300);
        setLayout(null);
        setResizable(false);
        setIconImage(Main.ICON);
        initComponent();
        initEvent();
        setLocationRelativeTo(null);
    }

    private void initComponent() {
        int buttonWidth = 250;
        int buttonHeight = 50;
        int yPadding = 50;
        int textWidth = 100;
        uploadButton.setBounds(getWidth() / 2 - (buttonWidth / 2), getHeight() / 2 - (buttonHeight / 2) + yPadding, buttonWidth, buttonHeight);
        downloadButton.setBounds(getWidth() / 2 - (buttonWidth / 2), getHeight() / 2 - (buttonHeight / 2) - (buttonHeight + 5) + yPadding, buttonWidth, buttonHeight);
        authorCredit.setBounds((getWidth() / 2) - (textWidth / 2), getHeight() - 60, textWidth, 10);
        dragAndDropPanel.setBounds(5, 5, getWidth() - 10, getHeight() / 3);
        dragAndDropPanel.setText("\n\nDrag and drop a .discloud file or\n a file/folder you wish to upload here");
        dragAndDropPanel.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent event) {
                event.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    List<File> droppedFiles = (List<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (droppedFiles.size() != 1) {
                        JOptionPane.showMessageDialog(null, "You can only upload 1 file/folder");
                        return;
                    }
                    File droppedFile = droppedFiles.get(0);
                    if (droppedFile.getName().endsWith(Main.FILE_SUFFIX)) {
                        new FileDownloaderFrame(droppedFile).setVisible(true);
                    } else {
                        new FileUploader(droppedFiles.get(0), prompt("Please enter your bot token."), prompt("Please enter the exact name of the text channel.")).uploadFiles();
                    }
                } catch (UnsupportedFlavorException | IOException | GeneralSecurityException |
                         ParseException exception) {
                    Util.reportError(exception, "Drag-and-drop failed");
                }
            }
        });

        StyledDocument styledDocument = dragAndDropPanel.getStyledDocument();
        dragAndDropPanel.setEditable(false);
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        styledDocument.setParagraphAttributes(0, styledDocument.getLength(), center, false);

        add(uploadButton);
        add(downloadButton);
        add(dragAndDropPanel);
        add(authorCredit);
    }


    private void initEvent() {
        uploadButton.addActionListener(this::uploadButtonClick);
        downloadButton.addActionListener(this::downloadButtonClick);
    }

    public String prompt(String prompt) {
        return (String) JOptionPane.showInputDialog(this, prompt, prompt, JOptionPane.PLAIN_MESSAGE, null, null, null);
    }

    public void uploadButtonClick(ActionEvent event) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setDialogTitle("Select a file/folder to upload");
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                new FileUploader(fileChooser.getSelectedFile(), prompt("Please enter your bot token."), prompt("Please enter the exact name of the text channel.")).uploadFiles();
            } catch (GeneralSecurityException exception) {
                Util.reportError(exception, "Upload failed");
            }
        }
    }

    private void downloadButtonClick(ActionEvent event) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.addChoosableFileFilter(new FileFilter() {
            public String getDescription() {
                return "Discloud files";
            }

            public boolean accept(File file) {
                return file.isDirectory() || file.getName().toLowerCase().endsWith(Main.FILE_SUFFIX);
            }
        });
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setDialogTitle("Select a " + Main.FILE_SUFFIX + " file to download");
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                new FileDownloaderFrame(fileChooser.getSelectedFile()).setVisible(true);
            } catch (GeneralSecurityException | IOException | ParseException exception) {
                exception.printStackTrace();
            }
        }
    }
}
