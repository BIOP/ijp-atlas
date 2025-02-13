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
package ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr.command;

import ch.epfl.biop.atlas.mouse.allen.ccfv3.AllenBrainCCFv3Downloader;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.AllenOntology;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.AllenBrainCCFv3p1Downloader;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr.AllenAtlasASR;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr.AllenBrainCCFv3p1ASRDownloader;
import ch.epfl.biop.atlas.struct.Atlas;
import ij.Prefs;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Plugin(type = Command.class)
public class AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand extends AllenAtlasASR implements Command {

	public String toString() {
		return getName();
	}
	
	public static String keyPrefix = AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.class.getName()+".";

	String mapUrl;// = Prefs.get(keyPrefix+"mapUrl","");
    String ontologyUrl;// = Prefs.get(keyPrefix+"ontologyUrl","");

	@Parameter(type= ItemIO.OUTPUT)
	Atlas ba;

	@Override
	public void run() {
        try {
        	URL mapURL, ontologyURL;
        	if ((mapUrl == null)||(mapUrl.isEmpty())||(ontologyUrl == null)||(ontologyUrl.isEmpty())) {
				mapURL = AllenBrainCCFv3p1ASRDownloader.getMapUrl();
				ontologyURL = AllenBrainCCFv3Downloader.getOntologyURL();
			} else {
				mapURL = new URL(mapUrl.replaceAll(" ", "%20"));
				ontologyURL = new URL(ontologyUrl.replaceAll(" ", "%20"));
			}
			try {
				this.initialize(mapURL, ontologyURL);
			} catch (Exception e) {
				System.err.println("Could not initialize the atlas : "+e.getMessage());
				System.err.println("Re-downloading it.");
				mapURL = AllenBrainCCFv3p1Downloader.getMapUrl();
				ontologyURL = AllenBrainCCFv3Downloader.getOntologyURL();
				try {
					this.initialize(mapURL, ontologyURL);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
			((AllenOntology)this.getOntology()).name = getName();

	        Prefs.set(keyPrefix + "mapUrl", mapURL.toString());
	        Prefs.set(keyPrefix + "ontologyUrl", ontologyURL.toString());

			ba=this; // put current object to output -> then processed by plugin

        } catch (Exception e) {
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

	final public static String atlasName = "allen_mouse_10um_java";

	@Override
	public String getName() {
		return atlasName;
	}
}
