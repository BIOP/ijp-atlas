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
