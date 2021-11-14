/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 EPFL
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
package ch.epfl.biop.atlas.mouse.allen.ccfv3.command;

import ch.epfl.biop.atlas.mouse.allen.ccfv3.AllenAtlas;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.AllenBrainCCFv3Downloader;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.AllenOntology;
import ch.epfl.biop.atlas.struct.Atlas;
import ij.Prefs;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

// Take ply files from : http://download.alleninstitute.org/informatics-archive/current-release/mouse_ccf/annotation/ccf_2017/structure_meshes/ply/

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>Allen Brain Adult Mouse Brain CCF 2017")
public class AllenBrainAdultMouseAtlasCCF2017Command extends AllenAtlas implements Command {

	public String toString() {
		return getName();
	}
	
	public static String keyPrefix = AllenBrainAdultMouseAtlasCCF2017Command.class.getName()+".";

	@Parameter(label = "URL path to allen brain map data, leave empty for downloading and caching", persist = false)
	String mapUrl = Prefs.get(keyPrefix+"mapUrl","");

	@Parameter(label = "URL path to allen brain ontology data, leave empty for downloading and caching", persist = false)
    String ontologyUrl = Prefs.get(keyPrefix+"ontologyUrl","");

	@Parameter(type= ItemIO.OUTPUT)
	Atlas ba;

	@Override
	public void run() {
        try {
        	URL mapURL, ontologyURL;
        	if ((mapUrl == null)||(mapUrl.equals(""))||(ontologyUrl == null)||(ontologyUrl.equals(""))) {
				mapURL = AllenBrainCCFv3Downloader.getMapUrl();
				ontologyURL = AllenBrainCCFv3Downloader.getOntologyURL();
			} else {
				mapURL = new URL(mapUrl);
				ontologyURL = new URL(ontologyUrl);
			}

			this.initialize(mapURL, ontologyURL);
			((AllenOntology)this.getOntology()).name = getName();

	        Prefs.set(keyPrefix + "mapUrl", mapURL.toString());
	        Prefs.set(keyPrefix + "ontologyUrl", ontologyURL.toString());

			ba=this; // put current object to output -> then processed by plugin

        } catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
	@Override
	public List<String> getDOIs() {
		List<String> dois = new ArrayList<>();
		dois.add("10.1016/j.cell.2020.04.007");
		return dois;
	}

	@Override
	public String getURL() {
		return "https://community.brain-map.org/t/allen-mouse-ccf-accessing-and-using-related-data-and-tools/359";
	}

	@Override
	public String getName() {
		return "Adult Mouse Brain - Allen Brain Atlas V3";
	}
}
