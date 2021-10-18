package ch.epfl.biop.atlas.mouse.allen.ccfv3;

import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import ch.epfl.biop.atlas.struct.Atlas;

import java.net.URL;

abstract public class AllenAtlas implements Atlas {
	// http://download.alleninstitute.org/informatics-archive/current-release/mouse_ccf/

	AllenOntology ontology;
	AllenMap map;

	@Override
	public void initialize(URL mapURL, URL ontologyURL) {
		ontology = new AllenOntology();
		ontology.setDataSource(ontologyURL);

		try {
			ontology.initialize();
			map = new AllenMap();
			map.setDataSource(mapURL);
			map.initialize(this.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
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
