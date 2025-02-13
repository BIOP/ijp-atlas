/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2025 EPFL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package ch.epfl.biop.atlas.mouse.allen.ccfv3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.BufferedOutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadProgressBar {

    protected static Logger logger = LoggerFactory.getLogger(DownloadProgressBar.class);

    public static void urlToFile(URL url, File file, String frameTitle, long fileSize) throws Exception {

        final JProgressBar jProgressBar = new JProgressBar();
        jProgressBar.setMaximum(10000);
        JFrame frame = new JFrame(frameTitle);
        frame.setContentPane(jProgressBar);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(600, 250);
        frame.setVisible(true);

        RunnableWithException updatethread = () -> {
            HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
            httpConnection.setRequestProperty("Accept", "*/*"); // Set Accept header

            int responseCode = httpConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Server returned HTTP response code: " + responseCode + " for URL: " + url);
            }

            long completeFileSize = httpConnection.getContentLengthLong();
            logger.info("File Size : " + completeFileSize);

            if (completeFileSize == -1) completeFileSize = fileSize;

            java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file.getAbsolutePath());
            BufferedOutputStream bout = new BufferedOutputStream(fos, 1024 * 1024);
            byte[] data = new byte[1024 * 1024];
            long downloadedFileSize = 0;
            int x = 0;
            while ((x = in.read(data, 0, 1024 * 1024)) >= 0) {
                downloadedFileSize += x;

                // calculate progress
                final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 10000);

                // update progress bar
                SwingUtilities.invokeLater(() -> jProgressBar.setValue(currentProgress));

                bout.write(data, 0, x);
            }
            bout.close();
            in.close();
        };

        Thread t = new Thread(() -> {
            try {
                updatethread.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
        t.join();
        frame.setVisible(false);
        frame.dispose();
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }

}
