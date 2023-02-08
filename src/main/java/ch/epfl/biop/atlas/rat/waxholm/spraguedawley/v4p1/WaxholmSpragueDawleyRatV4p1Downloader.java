/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2022 EPFL
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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p1;

import ch.epfl.biop.atlas.AtlasLocationHelper;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.DownloadProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class WaxholmSpragueDawleyRatV4p1Downloader {

    protected static Logger logger = LoggerFactory.getLogger(WaxholmSpragueDawleyRatV4p1Downloader.class);

    final public static String wh_sd_rat_v4p1_hdf5 = "https://zenodo.org/record/7492525/files/WHS_SD_rat_atlas_v4p1.h5?download=1";
    final public static String wh_sd_rat_v4p1_xml = "https://zenodo.org/record/7492525/files/WHS_SD_rat_atlas_v4p1.xml?download=1";
    final public static String wh_sd_rat_v4_ontology = "https://zenodo.org/record/5644162/files/WHS_SD_rat_atlas_v4_labels.ilf?download=1";

    static public URL getMapUrl() {
        if (!AtlasLocationHelper.getAtlasCacheDir().exists()) {
            AtlasLocationHelper.getAtlasCacheDir().mkdir();
        }

        File fileXml = new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4p1.xml");
        File fileHdf5 = new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4p1.h5");

        boolean dlH5 = true;
        boolean dlXml = true;

        if (fileHdf5.exists()) {
            if (fileHdf5.length() != 601_500_109L) {
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
            if (dlXml) DownloadProgressBar.urlToFile(new URL(wh_sd_rat_v4p1_xml), new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4p1.xml"), "Downloading WHS_SD_rat_atlas_v4p1.xml", -1);
            if (dlH5) DownloadProgressBar.urlToFile(new URL(wh_sd_rat_v4p1_hdf5), new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4p1.h5"), "Downloading WHS_SD_rat_atlas_v4p1.h5", 601_500_109L);

            returned = fileXml.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;
    }

    public static URL getOntologyURL() {
        File ontologyFile = new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4_labels.ilf");
        boolean dlOntology = true;
        if (ontologyFile.exists()) {
            dlOntology = false;
            logger.info("Ontology file already downloaded - skipping");
        }
        URL returned = null;

        try {
            if (dlOntology) DownloadProgressBar.urlToFile(new URL(wh_sd_rat_v4_ontology), new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4_labels.ilf"), "Downloading ontology", -1);

            returned = ontologyFile.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;

    }

}
