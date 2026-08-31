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
package ch.epfl.biop.atlas.mouse.allen.ccfv3;

import ch.epfl.biop.atlas.AtlasLocationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class AllenBrainCCFv3Downloader {

    protected static final Logger logger = LoggerFactory.getLogger(AllenBrainCCFv3Downloader.class);

    final public static String allen_mouse_brain_CCFv3_hdf5_v1 = "https://zenodo.org/record/4486659/files/ccf2017-mod65000-border-centered-mm-bc.h5?download=1";
    final public static long expected_hdf5_file_size = 3_089_344_351L;
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
            if (dlXml) AtlasLocationHelper.download(new URL(allen_mouse_brain_CCFv3_xml_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "mouse_brain_ccfv3.xml"), "Downloading mouse_brain_ccfv3.xml", -1);
            if (dlH5) AtlasLocationHelper.download(new URL(allen_mouse_brain_CCFv3_hdf5_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "ccf2017-mod65000-border-centered-mm-bc.h5"), "Downloading mouse_brain_ccfv3.h5", 3_089_344_351L);

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
            if (dlOntology) AtlasLocationHelper.download(new URL(allen_mouse_brain_CCFv3_ontology_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "1.json"), "Downloading ontology", -1);

            returned = ontologyFile.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;

    }

}
