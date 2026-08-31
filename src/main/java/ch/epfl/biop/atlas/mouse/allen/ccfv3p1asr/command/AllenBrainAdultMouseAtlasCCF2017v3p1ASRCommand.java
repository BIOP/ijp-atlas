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
	
	public static final String keyPrefix = AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.class.getName()+".";

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
