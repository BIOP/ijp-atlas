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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2asr;

import ch.epfl.biop.atlas.AtlasLocationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;

public class WaxholmSpragueDawleyRatV4p2ASRDownloader {

    protected static final Logger logger = LoggerFactory.getLogger(WaxholmSpragueDawleyRatV4p2ASRDownloader.class);

    private static final String hdf5FileName = "WHS_SD_rat_atlas_v4p1.h5";

    final public static String wh_sd_rat_v4p2_hdf5 = "https://zenodo.org/record/7492525/files/"+hdf5FileName+"?download=1";
    final public static String wh_sd_rat_v4p2_xml = "https://zenodo.org/records/14055690/files/WHS_SD_rat_atlas_v4p2asr.xml?download=1";
    static public URL getMapUrl() {
        if (!AtlasLocationHelper.getAtlasCacheDir().exists()) {
            AtlasLocationHelper.getAtlasCacheDir().mkdir();
        }

        String xmlFileName = "WHS_SD_rat_atlas_v4p2asr.xml";
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
            if (dlXml) AtlasLocationHelper.download(new URL(wh_sd_rat_v4p2_xml), new File(AtlasLocationHelper.getAtlasCacheDir(), xmlFileName), "Downloading "+ xmlFileName, -1);
            if (dlH5) AtlasLocationHelper.download(new URL(wh_sd_rat_v4p2_hdf5), new File(AtlasLocationHelper.getAtlasCacheDir(), hdf5FileName), "Downloading "+hdf5FileName, 601_500_109L);

            returned = fileXml.toURI().toURL();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returned;
    }

}
