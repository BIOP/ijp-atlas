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
package ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr;

import ch.epfl.biop.atlas.AtlasLocationHelper;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.AllenBrainCCFv3Downloader;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.DownloadProgressBar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class AllenBrainCCFv3p1ASRDownloader {

    protected static Logger logger = LoggerFactory.getLogger(AllenBrainCCFv3p1ASRDownloader.class);

    final public static String allen_mouse_brain_CCFv3p1asr_xml_v1 = "https://zenodo.org/records/14054361/files/mouse_brain_ccfv3p1asr.xml";

    static public URL getMapUrl() {
        if (!AtlasLocationHelper.getAtlasCacheDir().exists()) {
            AtlasLocationHelper.getAtlasCacheDir().mkdir();
        }

        File fileXml = new File(AtlasLocationHelper.getAtlasCacheDir(), "mouse_brain_ccfv3p1asr.xml");
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
            if (dlXml) DownloadProgressBar.urlToFile(new URL(allen_mouse_brain_CCFv3p1asr_xml_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "mouse_brain_ccfv3p1asr.xml"), "Downloading mouse_brain_ccfv3p1asr.xml", -1);
            if (dlH5) DownloadProgressBar.urlToFile(new URL(AllenBrainCCFv3Downloader.allen_mouse_brain_CCFv3_hdf5_v1), new File(AtlasLocationHelper.getAtlasCacheDir(), "ccf2017-mod65000-border-centered-mm-bc.h5"), "Downloading mouse_brain_ccfv3.h5", 3_089_344_351L);

            returned = fileXml.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;
    }

}
