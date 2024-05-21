/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2024 EPFL
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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2;

import ch.epfl.biop.atlas.AtlasLocationHelper;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.DownloadProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class WaxholmSpragueDawleyRatV4p2Downloader {

    protected static Logger logger = LoggerFactory.getLogger(WaxholmSpragueDawleyRatV4p2Downloader.class);

    private static String xmlFileName = "WHS_SD_rat_atlas_v4p2.xml";
    private static String hdf5FileName = "WHS_SD_rat_atlas_v4p1.h5";
    private static String ilfFileName = "WHS_SD_rat_atlas_v4_labels.ilf";

    final public static String wh_sd_rat_v4p2_hdf5 = "https://zenodo.org/record/7492525/files/"+hdf5FileName+"?download=1";
    final public static String wh_sd_rat_v4p2_xml = "https://zenodo.org/record/8092060/files/"+xmlFileName+"?download=1";
    final public static String wh_sd_rat_v4_ontology = "https://zenodo.org/record/5644162/files/"+ilfFileName+"?download=1";

    static public URL getMapUrl() {
        if (!AtlasLocationHelper.getAtlasCacheDir().exists()) {
            AtlasLocationHelper.getAtlasCacheDir().mkdir();
        }

        File fileXml = new File(AtlasLocationHelper.getAtlasCacheDir(), xmlFileName);
        File fileHdf5 = new File(AtlasLocationHelper.getAtlasCacheDir(), hdf5FileName);

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
            if (dlXml) DownloadProgressBar.urlToFile(new URL(wh_sd_rat_v4p2_xml), new File(AtlasLocationHelper.getAtlasCacheDir(), xmlFileName), "Downloading "+xmlFileName, -1);
            if (dlH5) DownloadProgressBar.urlToFile(new URL(wh_sd_rat_v4p2_hdf5), new File(AtlasLocationHelper.getAtlasCacheDir(), hdf5FileName), "Downloading "+hdf5FileName, 601_500_109L);

            returned = fileXml.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;
    }

    public static URL getOntologyURL() {
        File ontologyFile = new File(AtlasLocationHelper.getAtlasCacheDir(), ilfFileName);
        boolean dlOntology = true;
        if (ontologyFile.exists()) {
            dlOntology = false;
            logger.info("Ontology file already downloaded - skipping");
        }
        URL returned = null;

        try {
            if (dlOntology) DownloadProgressBar.urlToFile(new URL(wh_sd_rat_v4_ontology), new File(AtlasLocationHelper.getAtlasCacheDir(), ilfFileName), "Downloading ontology", -1);

            returned = ontologyFile.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;

    }

}
