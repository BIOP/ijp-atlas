/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2023 EPFL
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

import ch.epfl.biop.atlas.AtlasLocationHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;

public class AllenBrainCCFv3Downloader {

    protected static Logger logger = LoggerFactory.getLogger(AllenBrainCCFv3Downloader.class);

    final public static String allen_mouse_brain_CCFv3_hdf5_v1 = "https://zenodo.org/record/4486659/files/ccf2017-mod65000-border-centered-mm-bc.h5?download=1";
    final public static String allen_mouse_brain_CCFv3_xml_v1 = "https://zenodo.org/record/4486659/files/ccf2017-mod65000-border-centered-mm-bc.xml?download=1";
    final public static String allen_mouse_brain_CCFv3_ontology_v1 = "https://zenodo.org/record/4486659/files/1.json?download=1";

    static public URL getMapUrl() {
        if (!AtlasLocationHelper.getAtlasCacheDir().exists()) {
            AtlasLocationHelper.getAtlasCacheDir().mkdir();
        }

        File fileXml = new File(AtlasLocationHelper.getAtlasCacheDir(), "mouse_brain_ccfv3.xml");
        File fileHdf5 = new File(AtlasLocationHelper.getAtlasCacheDir(), "ccf2017-mod65000-border-centered-mm-bc.h5");

        boolean dlH5 = true;
        boolean dlXml = true;

        if (fileHdf5.exists()) {
            if (fileHdf5.length() != 3_089_344_351L) {
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
            if (dlXml) DownloadProgressBar.urlToFile(new URL(allen_mouse_brain_CCFv3_xml_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "mouse_brain_ccfv3.xml"), "Downloading mouse_brain_ccfv3.xml", -1);
            if (dlH5) DownloadProgressBar.urlToFile(new URL(allen_mouse_brain_CCFv3_hdf5_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "ccf2017-mod65000-border-centered-mm-bc.h5"), "Downloading mouse_brain_ccfv3.h5", 3_089_344_351L);

            returned = fileXml.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;
    }

    public static URL getOntologyURL() {
        File ontologyFile = new File(AtlasLocationHelper.getAtlasCacheDir(), "1.json");
        boolean dlOntology = true;
        if (ontologyFile.exists()) {
            dlOntology = false;
            logger.info("Ontology file already downloaded - skipping");
        }
        URL returned = null;

        try {
            if (dlOntology) DownloadProgressBar.urlToFile(new URL(allen_mouse_brain_CCFv3_ontology_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "1.json"), "Downloading ontology", -1);

            returned = ontologyFile.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;

    }

}
