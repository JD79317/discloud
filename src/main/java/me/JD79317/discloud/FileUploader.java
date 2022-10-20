package me.JD79317.discloud;

import me.JD79317.discloud.frames.ConsoleFrame;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

import static me.JD79317.discloud.FileSplitter.SPLIT_SIZE;
import static me.JD79317.discloud.Main.EXECUTOR;
import static me.JD79317.discloud.Main.clearTmpDirectory;
import static me.JD79317.discloud.Util.*;

public final class FileUploader {
    private final Cipher cipher;
    private final ConsoleFrame console;
    private final File selectedFile;
    private final ArrayList<File> resolvedFiles = new ArrayList<>();
    private final DiscloudData data;
    private final String botToken;
    private final String channelName;
    private CountDownLatch latch;
    private long fileCount;
    private int chunkCount;
    private long totalSize;

    public FileUploader(File selectedFile, String botToken, String channelName) throws GeneralSecurityException {
        this.botToken = botToken;
        this.channelName = channelName;
        this.selectedFile = selectedFile;
        this.console = new ConsoleFrame(true);

        SecretKey secretKey = KeyGenerator.getInstance("AES").generateKey();
        String key = Base64.getEncoder().encodeToString(secretKey.getEncoded());

        this.cipher = getCipher(secretKey, null);
        console.log("File encryption key: " + key);

        if (selectedFile.isDirectory()) {
            resolveFiles(selectedFile, resolvedFiles);
        } else {
            resolvedFiles.add(selectedFile);
        }

        List<String> extraDirectories = new ArrayList<>();
        // Calculate total number of chunks
        for (File file : resolvedFiles) {
            if (!file.isDirectory()) {
                long length = file.length();
                chunkCount += Math.ceil(length / (double) SPLIT_SIZE);
                totalSize += length;
                fileCount++;
            } else {
                extraDirectories.add(getRelativePath(file, selectedFile));
            }
        }

        data = new DiscloudData(new HashMap<>(), key, Base64.getEncoder().encodeToString(cipher.getIV()), System.currentTimeMillis(), selectedFile.isDirectory() ? selectedFile.getName() : null, extraDirectories, 0);
    }

    public void uploadFiles() {
        EXECUTOR.execute(() -> {
            try {
                File saveFile = saveFile(selectedFile.getName(), console);
                if (saveFile == null) {
                    return;
                }
                console.setVisible(true);
                console.log("Loading discord bot...");
                JDA discordBot = DiscordBotManager.init(botToken);
                if (discordBot == null) {
                    console.setVisible(false);
                    return;
                }

                this.latch = new CountDownLatch(chunkCount);

                console.log("Uploading " + fileCount + " files in " + chunkCount + " chunks to discord CDN (" + Math.round(totalSize / 1024D / 1024D) + "MB)");
                data.setSize(totalSize);
                List<SplitFile> currentAttachments = new ArrayList<>();

                console.log("Splitting and encrypting files...");
                for (File file : resolvedFiles) {
                    if (!file.isDirectory()) {
                        currentAttachments.add(new SplitFile(file, FileSplitter.split(file, cipher)));
                        int sum = currentAttachments.stream().mapToInt(currentAttachment -> currentAttachment.parts.size()).sum();
                        if (sum >= DiscordBotManager.MAX_ATTACHMENTS_PER_MESSAGE) {
                            sendFiles(currentAttachments, discordBot);
                        }
                    }
                }

                while (!currentAttachments.isEmpty()) {
                    sendFiles(currentAttachments, discordBot);
                }

                long lastCount = 0;
                while (latch.getCount() > 0) {
                    long count = latch.getCount();
                    if (count != lastCount) {
                        lastCount = count;
                        console.log("  " + (Math.round(((double) (chunkCount - count) / chunkCount) * 100)) + "% complete...");
                    }
                    Thread.sleep(100);
                }
                console.log("Writing save file...");
                try (FileWriter file = new FileWriter(saveFile)) {
                    file.write(data.serialize());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                console.log("Cleaning up temporary files...");
                clearTmpDirectory();
                console.log("Done!");
                console.setFinished(true);
                displayTray("Upload Complete", "Upload of " + fileCount + " files have completed.");
            } catch (Throwable throwable) {
                console.setVisible(false);
                reportError(throwable, "An error occurred whilst uploading files");
            }
        });
    }

    private void sendFiles(List<FileUploader.SplitFile> files, JDA discordBot) {
        List<TextChannel> channels = discordBot.getTextChannelsByName(channelName, true);
        if (channels.isEmpty()) {
            reportError(new Exception(), "Text channel with name '" + channelName + "' not found");
            return;
        }
        MessageAction messageAction = channels.get(0).sendMessage(".");
        Iterator<SplitFile> splitFileIterator = files.iterator();
        HashMap<String, String> partsToOriginal = new HashMap<>();

        splitIterator:
        // Is there a better way to implement this??
        while (splitFileIterator.hasNext()) {
            FileUploader.SplitFile file = splitFileIterator.next();
            Iterator<File> partsIterator = file.parts().iterator();

            while (partsIterator.hasNext()) {
                File part = partsIterator.next();
                messageAction = messageAction.addFile(part, part.getName());
                partsIterator.remove();
                partsToOriginal.put(part.getName(), getRelativePath(file.originalName(), selectedFile));
                if (partsToOriginal.size() >= DiscordBotManager.MAX_ATTACHMENTS_PER_MESSAGE) {
                    break splitIterator;
                }
            }

            if (file.parts().isEmpty()) {
                splitFileIterator.remove();
            }
        }
        queue(messageAction, partsToOriginal, false, files);
    }

    private void queue(MessageAction messageAction, HashMap<String, String> partsToOriginal, boolean repeat, List<FileUploader.SplitFile> files) {
        if (repeat) {
            console.log("Trying again to send: " + partsToOriginal.keySet().iterator().next());
        }
        messageAction.queue(message -> {
            List<Message.Attachment> attachments = message.getAttachments();
            for (Message.Attachment attachment : attachments) {
                String originalFile = partsToOriginal.get(attachment.getFileName());
                List<String> urls = data.getUrlPath().getOrDefault(originalFile, new ArrayList<>());
                urls.add(attachment.getUrl());
                data.getUrlPath().put(originalFile, urls);
                latch.countDown();
            }
            console.log("Shipped message with " + attachments.size() + " attachments: " + partsToOriginal.keySet().iterator().next());
        }, throwable -> {
            throwable.printStackTrace();
            queue(messageAction, partsToOriginal, true, files);
        });
    }

    record SplitFile(File originalName, List<File> parts) {
    }
}
