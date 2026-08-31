/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2026 EPFL and University of Geneva
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */
package ch.epfl.biop.atlas;

import ij.IJ;
import org.apache.commons.io.IOUtils;
import org.scijava.Context;
import org.scijava.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AtlasLocationHelper {

    protected static final Logger logger = LoggerFactory.getLogger(AtlasLocationHelper.class);

    private static Context ctx;

    public static void setContext(Context ctx) {
        AtlasLocationHelper.ctx = ctx;
    }

    public static Context getContext() {
        return ctx;
    }

    public static File defaultCacheDir = null;

    public static File getAtlasCacheDir() {

        if (defaultCacheDir!=null) {
            return defaultCacheDir;
        }

        File f = new File("./plugins/BIOP/ABBA_Atlas_folder.txt");
        if (f.exists()) {
            FileInputStream fisTargetFile;
            try {
                fisTargetFile = new FileInputStream(f);
                String targetFileStr = IOUtils.toString(fisTargetFile, StandardCharsets.UTF_8);
                File dir = new File(targetFileStr);
                if (dir.exists()) {
                    return dir;
                } else {
                    // Attempt to make it
                    boolean result = dir.mkdir();
                    if (result) {
                        return dir;
                    } else {
                        System.out.println("Couldn't create folder for caching atlases "+dir.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Default behaviour
        return new File(System.getProperty("user.home"),"cached_atlas");
    }

    /**
     * Downloads a file from a URL with progress reported via SciJava TaskService
     * and ImageJ status/progress bar. Falls back to logging-only if no Context is set.
     *
     * @param url          the URL to download from
     * @param file         the destination file
     * @param taskName     display name for the progress task
     * @param expectedSize expected file size in bytes, or -1 if unknown
     * @throws Exception if the download fails
     */
    public static void download(URL url, File file, String taskName, long expectedSize) throws Exception {
        org.scijava.task.Task downloadTask = null;
        if (ctx != null) {
            try {
                downloadTask = ctx.getService(TaskService.class).createTask(taskName);
                downloadTask.start();
            } catch (Exception e) {
                logger.warn("Could not create task for download progress", e);
            }
        }

        try {
            HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
            httpConnection.setRequestProperty("Accept", "*/*");

            int responseCode = httpConnection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Server returned HTTP response code: " + responseCode + " for URL: " + url);
            }

            long completeFileSize = httpConnection.getContentLengthLong();
            logger.info("File Size : " + completeFileSize);

            if (completeFileSize == -1) completeFileSize = expectedSize;

            if (downloadTask != null && completeFileSize > 0) {
                downloadTask.setProgressMaximum(completeFileSize);
                IJ.showStatus(taskName);
            }

            BufferedInputStream in = new BufferedInputStream(httpConnection.getInputStream());
            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
            BufferedOutputStream bout = new BufferedOutputStream(fos, 1024 * 1024);
            byte[] data = new byte[1024 * 1024];
            long downloadedFileSize = 0;
            int x;
            while ((x = in.read(data, 0, 1024 * 1024)) >= 0) {
                downloadedFileSize += x;

                if (downloadTask != null && completeFileSize > 0) {
                    downloadTask.setProgressValue(downloadedFileSize);
                    IJ.showProgress((int) (downloadedFileSize / 1024), (int) (completeFileSize / 1024));
                }

                bout.write(data, 0, x);
            }
            bout.close();
            in.close();
        } finally {
            if (downloadTask != null) {
                downloadTask.finish();
            }
        }
    }
}
