/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2026 EPFL
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
