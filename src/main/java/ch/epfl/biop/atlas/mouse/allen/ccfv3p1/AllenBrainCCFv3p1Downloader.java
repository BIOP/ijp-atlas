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
package ch.epfl.biop.atlas.mouse.allen.ccfv3p1;

import ch.epfl.biop.atlas.AtlasLocationHelper;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.AllenBrainCCFv3Downloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class AllenBrainCCFv3p1Downloader {

    protected static final Logger logger = LoggerFactory.getLogger(AllenBrainCCFv3p1Downloader.class);

    final public static String allen_mouse_brain_CCFv3p1_xml_v1 = "https://zenodo.org/record/7492551/files/ccf2017-mod65000-border-centered-mm-bc.xml?download=1";

    static public URL getMapUrl() {
        if (!AtlasLocationHelper.getAtlasCacheDir().exists()) {
            AtlasLocationHelper.getAtlasCacheDir().mkdir();
        }

        File fileXml = new File(AtlasLocationHelper.getAtlasCacheDir(), "mouse_brain_ccfv3p1.xml");
        File fileHdf5 = new File(AtlasLocationHelper.getAtlasCacheDir(), "ccf2017-mod65000-border-centered-mm-bc.h5");

        boolean dlH5 = true;
        boolean dlXml = true;

        if (fileHdf5.exists()) {
            if (fileHdf5.length() != AllenBrainCCFv3Downloader.expected_hdf5_file_size) {
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
            if (dlXml) AtlasLocationHelper.download(new URL(allen_mouse_brain_CCFv3p1_xml_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "mouse_brain_ccfv3p1.xml"), "Downloading mouse_brain_ccfv3p1.xml", -1);
            if (dlH5) AtlasLocationHelper.download(new URL(AllenBrainCCFv3Downloader.allen_mouse_brain_CCFv3_hdf5_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "ccf2017-mod65000-border-centered-mm-bc.h5"), "Downloading mouse_brain_ccfv3.h5", 3_089_344_351L);

            returned = fileXml.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;
    }

}
