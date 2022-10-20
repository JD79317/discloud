package me.JD79317.discloud;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Super-fast file splitter inspired by <a href="http://todayguesswhat.blogspot.com/2014/05/java-split-large-file-sample-code-high.html">this blog post</a>
 */
public final class FileSplitter {
    public static final File TMP_FOLDER = new File(System.getProperty("java.io.tmpdir"), "discloud_working");
    public static final int SPLIT_SIZE = 8388284; // 8388284 is the max discord upload limit

    private FileSplitter() {
    }

    public static List<File> split(File input, Cipher cipher) throws IOException {
        TMP_FOLDER.mkdir();

        List<File> splitFiles = new ArrayList<>();

        try (FileChannel sourceChannel = new FileInputStream(input).getChannel()) {

            ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024 * 1024); // 16MB Buffer for reading source file

            WritableByteChannel outputChannel = null;
            try {
                long totalBytesWritten = 0; // total bytes written to output
                long outputChunkBytesWritten = 0; // number of bytes written to chunk so far
                int chunk = 0; // the current chunk
                for (int bytesRead = sourceChannel.read(buffer); bytesRead != -1; bytesRead = sourceChannel.read(buffer)) {
                    buffer.flip(); // convert the buffer from writing data to buffer from disk to reading mode
                    int bytesWrittenFromBuffer = 0; // number of bytes written from buffer

                    while (buffer.hasRemaining()) {
                        if (outputChannel == null) {
                            outputChunkBytesWritten = 0;

                            File chunkFile = new File(TMP_FOLDER, chunk + "_" + UUID.randomUUID().toString().replace("-", "") + ".zip");
                            chunkFile.deleteOnExit();
                            splitFiles.add(chunkFile);
                            outputChannel = Channels.newChannel(new CipherOutputStream(new FileOutputStream(chunkFile), cipher));
                        }
                        long chunkBytesFree = (SPLIT_SIZE - outputChunkBytesWritten); // maximum free space in chunk
                        int bytesToWrite = (int) Math.min(buffer.remaining(), chunkBytesFree); // maximum bytes that should be read from current byte buffer
                        buffer.limit(bytesWrittenFromBuffer + bytesToWrite); // set limit in buffer up to where bytes can be read

                        int bytesWritten = outputChannel.write(buffer);
                        outputChunkBytesWritten += bytesWritten;
                        bytesWrittenFromBuffer += bytesWritten;
                        totalBytesWritten += bytesWritten;
                        buffer.limit(bytesRead); // reset limit

                        if (totalBytesWritten == sourceChannel.size()) {
                            closeChannel(outputChannel);
                            outputChannel = null;
                            break;
                        } else if (outputChunkBytesWritten == SPLIT_SIZE) {
                            closeChannel(outputChannel);
                            outputChannel = null;
                            chunk++;
                        }
                    }
                    buffer.clear();
                }
            } finally {
                closeChannel(outputChannel);
            }
        }
        return splitFiles;
    }

    private static void closeChannel(WritableByteChannel channel) {
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception ignored) {
            }
        }
    }
}