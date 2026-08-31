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
package ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr;

import ch.epfl.biop.atlas.mouse.allen.ccfv3.AllenOntology;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasOntology;

import java.net.URL;

/**
 * A version of the adult mouse Allen Brain Atlas CCFv3 which is matching the BrainGlobe convention
 * In ASR orientation in space
 */
abstract public class AllenAtlasASR implements Atlas {
	// http://download.alleninstitute.org/informatics-archive/current-release/mouse_ccf/

	AllenOntology ontology;
	AllenMapASR map;

	@Override
	public void initialize(URL mapURL, URL ontologyURL) throws Exception{
		ontology = new AllenOntology();
		ontology.setDataSource(ontologyURL);

		ontology.initialize();
		map = new AllenMapASR();
		map.setDataSource(mapURL);
		map.initialize(this.toString());
	}

	@Override
	public AtlasOntology getOntology() {
		return ontology;
	}

	@Override
	public AtlasMap getMap() {
		return map;
	}

}
