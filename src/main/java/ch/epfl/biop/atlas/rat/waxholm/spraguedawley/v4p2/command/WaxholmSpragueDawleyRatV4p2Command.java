package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.command;
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

import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.AllenBrainCCFv3p1Downloader;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.WaxholmSpragueDawleyRatV4p2Atlas;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.WaxholmSpragueDawleyRatV4p2Downloader;
import ch.epfl.biop.atlas.struct.Atlas;
import ij.Prefs;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.net.MalformedURLException;
import java.net.URL;

@Plugin(type = Command.class)//, menuPath = "Plugins>BIOP>Atlas>Rat (Waxholm Sprague Dawley V4p1)")
public class WaxholmSpragueDawleyRatV4p2Command extends WaxholmSpragueDawleyRatV4p2Atlas implements Command {

	/*public static String atlasName = "Rat - Waxholm Sprague Dawley V4p1";

	public String toString() {
		return atlasName;
	}*/

    public static String keyPrefix = WaxholmSpragueDawleyRatV4p2Command.class.getName()+".";

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
            if ((mapUrl == null)||(mapUrl.equals(""))||(ontologyUrl == null)||(ontologyUrl.equals(""))) {
                mapURL = WaxholmSpragueDawleyRatV4p2Downloader.getMapUrl();
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
                mapURL = WaxholmSpragueDawleyRatV4p2Downloader.getMapUrl();
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
