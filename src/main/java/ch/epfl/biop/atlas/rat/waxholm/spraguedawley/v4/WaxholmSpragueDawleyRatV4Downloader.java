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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4;

import ch.epfl.biop.atlas.AtlasLocationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class WaxholmSpragueDawleyRatV4Downloader {

    protected static final Logger logger = LoggerFactory.getLogger(WaxholmSpragueDawleyRatV4Downloader.class);

    final public static String wh_sd_rat_v4_hdf5 = "https://zenodo.org/record/5644162/files/WHS_SD_rat_atlas_v4.h5?download=1";
    final public static String wh_sd_rat_v4_xml = "https://zenodo.org/record/5644162/files/WHS_SD_rat_atlas_v4.xml?download=1";
    final public static String wh_sd_rat_v4_ontology = "https://zenodo.org/record/5644162/files/WHS_SD_rat_atlas_v4_labels.ilf?download=1";

    static public URL getMapUrl() {
        if (!AtlasLocationHelper.getAtlasCacheDir().exists()) {
            AtlasLocationHelper.getAtlasCacheDir().mkdir();
        }

        File fileXml = new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4.xml");
        File fileHdf5 = new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4.h5");

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
            if (dlXml) AtlasLocationHelper.download(new URL(wh_sd_rat_v4_xml), new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4.xml"), "Downloading WHS_SD_rat_atlas_v4.xml", -1);
            if (dlH5) AtlasLocationHelper.download(new URL(wh_sd_rat_v4_hdf5), new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4.h5"), "Downloading WHS_SD_rat_atlas_v4.h5", 499_413_218L);

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
            if (dlOntology) AtlasLocationHelper.download(new URL(wh_sd_rat_v4_ontology), new File(AtlasLocationHelper.getAtlasCacheDir(), "WHS_SD_rat_atlas_v4_labels.ilf"), "Downloading ontology", -1);

            returned = ontologyFile.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;

    }

}
