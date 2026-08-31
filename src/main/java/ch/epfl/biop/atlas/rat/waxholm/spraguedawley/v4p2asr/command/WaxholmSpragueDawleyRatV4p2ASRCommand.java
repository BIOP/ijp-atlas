package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2asr.command;
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

import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.WaxholmSpragueDawleyRatV4p2Downloader;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2asr.WaxholmSpragueDawleyRatV4p2ASRAtlas;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2asr.WaxholmSpragueDawleyRatV4p2ASRDownloader;
import ch.epfl.biop.atlas.struct.Atlas;
import ij.Prefs;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.net.MalformedURLException;
import java.net.URL;

@Plugin(type = Command.class)//, menuPath = "Plugins>BIOP>Atlas>Rat (Waxholm Sprague Dawley V4p1)")
public class WaxholmSpragueDawleyRatV4p2ASRCommand extends WaxholmSpragueDawleyRatV4p2ASRAtlas implements Command {

    public static String keyPrefix = WaxholmSpragueDawleyRatV4p2ASRCommand.class.getName()+".";

    //@Parameter(label = "URL path to brain map data, leave empty for downloading and caching", persist = false)
    String mapUrl = Prefs.get(keyPrefix+"mapUrl","");

    //@Parameter(label = "URL path to brain ontology data, leave empty for downloading and caching", persist = false)
    String ontologyUrl = Prefs.get(keyPrefix+"ontologyUrl","");

    @Parameter(type= ItemIO.OUTPUT)
    Atlas ba;

    @Override
    public void run() {
        try {
            URL mapURL, ontologyURL;
            if ((mapUrl == null)||(mapUrl.isEmpty())||(ontologyUrl == null)||(ontologyUrl.isEmpty())) {
                mapURL = WaxholmSpragueDawleyRatV4p2ASRDownloader.getMapUrl();
                ontologyURL = WaxholmSpragueDawleyRatV4p2Downloader.getOntologyURL();
            } else
            {
                mapURL = new URL(mapUrl.replaceAll(" ", "%20"));
                ontologyURL = new URL(ontologyUrl.replaceAll(" ", "%20"));
            }
            try {
                this.initialize(mapURL, ontologyURL);
            } catch (Exception e) {
                System.err.println("Could not initialize the atlas : "+e.getMessage());
                System.err.println("Re-downloading it : "+e.getMessage());
                mapURL = WaxholmSpragueDawleyRatV4p2ASRDownloader.getMapUrl();
                ontologyURL = WaxholmSpragueDawleyRatV4p2Downloader.getOntologyURL();
                try {
                    this.initialize(mapURL, ontologyURL);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }

            Prefs.set(keyPrefix + "mapUrl", mapURL.toString());
            Prefs.set(keyPrefix + "ontologyUrl", ontologyURL.toString());

            ba=this; // put current object to output -> then processed by plugin

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }


}
