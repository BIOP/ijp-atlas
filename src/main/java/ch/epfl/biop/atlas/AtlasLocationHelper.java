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
package ch.epfl.biop.atlas;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;

public class AtlasLocationHelper {

    //public static File cachedSampleDir = getAtlasCacheDir();//

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
                String targetFileStr = IOUtils.toString(fisTargetFile, "UTF-8");
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
}
