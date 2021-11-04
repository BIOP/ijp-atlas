package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4;

import ch.epfl.biop.atlas.mouse.allen.ccfv3.DownloadProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class WaxholmSpragueDawleyRatV4Downloader {

    protected static Logger logger = LoggerFactory.getLogger(WaxholmSpragueDawleyRatV4Downloader.class);

    final public static String wh_sd_rat_v4_hdf5 = "https://zenodo.org/record/5644162/files/WHS_SD_rat_atlas_v4.h5?download=1";
    final public static String wh_sd_rat_v4_xml = "https://zenodo.org/record/5644162/files/WHS_SD_rat_atlas_v4.xml?download=1";
    final public static String wh_sd_rat_v4_ontology = "https://zenodo.org/record/5644162/files/WHS_SD_rat_atlas_v4_labels.ilf?download=1";

    public static File cachedSampleDir = new File(System.getProperty("user.home"),"cached_atlas");

    static public URL getMapUrl() {
        if (!cachedSampleDir.exists()) {
            cachedSampleDir.mkdir();
        }

        File fileXml = new File(cachedSampleDir, "WHS_SD_rat_atlas_v4.xml");
        File fileHdf5 = new File(cachedSampleDir, "WHS_SD_rat_atlas_v4.h5");

        boolean dlH5 = true;
        boolean dlXml = true;

        if (fileHdf5.exists()) {
            if (fileHdf5.length() != 499_413_218L) {
                logger.warn("hdf5 file wrong size ... downloading again");
            } else {
                logger.info("hdf5 file already downloaded - skipping");
                dlH5 = false;
            }
        }

        if (fileXml.exists()) {
            logger.info("xml file already downloaded - skipping");
            dlXml = false;
        }

        URL returned = null;

        try {
            if (dlXml) DownloadProgressBar.urlToFile(new URL(wh_sd_rat_v4_xml), new File(cachedSampleDir, "WHS_SD_rat_atlas_v4.xml"), "Downloading WHS_SD_rat_atlas_v4.xml", -1);
            if (dlH5) DownloadProgressBar.urlToFile(new URL(wh_sd_rat_v4_hdf5), new File(cachedSampleDir, "WHS_SD_rat_atlas_v4.h5"), "Downloading WHS_SD_rat_atlas_v4.h5", 499_413_218L);

            returned = fileXml.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;
    }

    public static URL getOntologyURL() {
        File ontologyFile = new File(cachedSampleDir, "WHS_SD_rat_atlas_v4_labels.ilf");
        boolean dlOntology = true;
        if (ontologyFile.exists()) {
            dlOntology = false;
            logger.info("Ontology file already downloaded - skipping");
        }
        URL returned = null;

        try {
            if (dlOntology) DownloadProgressBar.urlToFile(new URL(wh_sd_rat_v4_ontology), new File(cachedSampleDir, "WHS_SD_rat_atlas_v4_labels.ilf"), "Downloading ontology", -1);

            returned = ontologyFile.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;

    }

}
