package me.JD79317.discloud.frames;

import me.JD79317.discloud.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.PrintStream;

public final class ConsoleFrame extends JFrame {
    private final String type;
    private boolean finished = false;
    private transient PrintStream logger;
    private JTextArea textArea;

    public ConsoleFrame(boolean upload) {
        this.type = upload ? "Upload" : "Download";
        setTitle(type + " Window");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setIconImage(Main.ICON);
        setLocationRelativeTo(null);

        getContentPane().add(new JPanel() {{
            setLayout(new BorderLayout());
            textArea = new JTextArea(20, 80);
            textArea.setEditable(false);
            add(new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
            logger = new PrintStream(new TextAreaOutputStream());
        }});

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!finished) {
                    log("Cannot close window. " + type + " task is still running.");
                } else {
                    dispose();
                }
            }
        });
        pack();
    }

    public void log(String string) {
        logger.println(string);
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public class TextAreaOutputStream extends OutputStream {
        private final StringBuilder stringBuilder = new StringBuilder();

        @Override
        public void write(int b) {
            if (b == '\r') {
                return;
            }
            if (b == '\n') {
                final String text = stringBuilder + "\n";
                SwingUtilities.invokeLater(() -> textArea.append(text));
                stringBuilder.setLength(0);
                return;
            }
            stringBuilder.append((char) b);
        }
    }
}
