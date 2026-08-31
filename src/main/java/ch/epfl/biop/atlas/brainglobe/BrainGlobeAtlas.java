/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2026 EPFL
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
package ch.epfl.biop.atlas.brainglobe;

import ch.epfl.biop.atlas.AtlasLocationHelper;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;

import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Atlas implementation backed by the BrainGlobe Atlas API via Appose.
 * <p>
 * This class orchestrates:
 * 1. Creating/reusing a Python environment with brainglobe-atlasapi
 * 2. Fetching atlas data (images + ontology) from BrainGlobe
 * 3. Building the AtlasMap (BDV sources) and AtlasOntology from the data
 * <p>
 * Usage:
 * <pre>
 *   BrainGlobeAtlas atlas = new BrainGlobeAtlas("example_mouse_100um@3.1");
 *   atlas.initialize(null, null); // URLs not used, BrainGlobe handles data source
 * </pre>
 * The version is mandatory; see {@link BrainGlobeAtlasId}.
 */
public class BrainGlobeAtlas implements Atlas {

	private final BrainGlobeAtlasId id;
	private final BrainGlobeAppose appose;

	private BrainGlobeAtlasMap atlasMap;
	private AtlasOntology ontology;
	private String atlasLink = "";
	private final List<String> dois = new ArrayList<>();

	private Consumer<String> progressCallback;

	/**
	 * @param bgAtlasId versioned atlas id, e.g. {@code allen_mouse_50um@3.1}
	 * @throws IllegalArgumentException if no version is given
	 */
	public BrainGlobeAtlas(String bgAtlasId) {
		this(BrainGlobeAtlasId.parse(bgAtlasId), new BrainGlobeAppose());
	}

	public BrainGlobeAtlas(BrainGlobeAtlasId id) {
		this(id, new BrainGlobeAppose());
	}

	public BrainGlobeAtlas(BrainGlobeAtlasId id, BrainGlobeAppose appose) {
		this.id = id;
		this.appose = appose;
	}

	public void setProgressCallback(Consumer<String> callback) {
		this.progressCallback = callback;
		appose.setProgressCallback(callback);
	}

	public void setErrorCallback(Consumer<String> callback) {
		appose.setErrorCallback(callback);
	}

	@Override
	public void initialize(URL mapURL, URL ontologyURL) throws Exception {
		if (progressCallback != null) {
			progressCallback.accept("Fetching BrainGlobe atlas: " + id);
		}

		// Fetch all data from Python via Appose
		BrainGlobeAppose.BrainGlobeAtlasData data = appose.fetchAtlas(id);

		// Build the ontology from structures JSON
		ontology = buildOntology(data);

		// Build the map (BDV sources) from file paths
		atlasMap = new BrainGlobeAtlasMap();
		atlasMap.initializeFromApposeData(data, AtlasLocationHelper.getContext());

		// Extract metadata
		atlasLink = data.getAtlasLink();
		String citation = data.getCitation();
		if (citation != null && !citation.isEmpty()) {
			try {
				String doi = citation.split("doi.org/")[1];
				dois.add(doi);
			} catch (Exception e) {
				dois.add("could not parse doi");
			}
		}
	}

	private AtlasOntology buildOntology(BrainGlobeAppose.BrainGlobeAtlasData data) throws Exception {
		// Write structures JSON to a temp file for BrainGlobeHelper
		File tempStructures = File.createTempFile("bg_structures_", ".json");
		tempStructures.deleteOnExit();
		try (FileWriter fw = new FileWriter(tempStructures)) {
			fw.write(data.structuresJson);
		}

		// Build the ontology tree using existing BrainGlobeHelper
		AtlasNode root = BrainGlobeHelper.buildTreeAndGetRoot(tempStructures.getAbsolutePath());
		Map<Integer, AtlasNode> idToNode = AtlasHelper.buildIdToAtlasNodeMap(root);

		return new BrainGlobeOntology(id.toString(), root, idToNode);
	}

	@Override
	public AtlasMap getMap() {
		return atlasMap;
	}

	@Override
	public AtlasOntology getOntology() {
		return ontology;
	}

	@Override
	public List<String> getDOIs() {
		return dois;
	}

	@Override
	public String getURL() {
		return atlasLink;
	}

	@Override
	public String getName() {
		return id.toString();
	}

	@Override
	public String toString() {
		return getName();
	}

	// --- Inner ontology class ---

	private static class BrainGlobeOntology implements AtlasOntology {

		private final String name;
		private final AtlasNode root;
		private final Map<Integer, AtlasNode> idToNode;
		private String namingProperty = "acronym";
		private URL dataSource;

		BrainGlobeOntology(String name, AtlasNode root, Map<Integer, AtlasNode> idToNode) {
			this.name = name;
			this.root = root;
			this.idToNode = idToNode;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public void initialize() {
			// Already initialized in constructor
		}

		@Override
		public void setDataSource(URL dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public URL getDataSource() {
			return dataSource;
		}

		@Override
		public AtlasNode getRoot() {
			return root;
		}

		@Override
		public AtlasNode getNodeFromId(int id) {
			return idToNode.get(id);
		}

		@Override
		public String getNamingProperty() {
			return namingProperty;
		}

		@Override
		public void setNamingProperty(String namingProperty) {
			this.namingProperty = namingProperty;
		}
	}
}
