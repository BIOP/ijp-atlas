package ch.epfl.biop.atlas;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;

public class AtlasLocationHelper {

    public static File cachedSampleDir = getAtlasCacheDir();//

    private static File getAtlasCacheDir() {
        File f = new File("./plugins/BIOP/ABBA_Atlas_folder.txt");
        if (f.exists()) {
            FileInputStream fisTargetFile;
            try {
                fisTargetFile = new FileInputStream(f);
                String targetFileStr = IOUtils.toString(fisTargetFile, "UTF-8");
                File dir = new File(targetFileStr);
                if (dir.exists()) {
                    return f;
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
