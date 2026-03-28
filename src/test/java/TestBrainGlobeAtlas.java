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
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAtlas;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;

/**
 * Manual test for BrainGlobeAtlas full pipeline.
 * <p>
 * Tests the complete flow: Appose Python fetch -> AtlasMap (BDV sources) + AtlasOntology.
 * Uses "example_mouse_100um" which is a small test atlas.
 * <p>
 * Run with: mvn exec:java -Dexec.mainClass="TestBrainGlobeAtlas"
 */
public class TestBrainGlobeAtlas {

	public static void main(String[] args) throws Exception {

		System.out.println("=== Test: Full BrainGlobeAtlas pipeline ===\n");

		// Create and initialize the atlas
		BrainGlobeAtlas atlas = new BrainGlobeAtlas("example_mouse_100um");
		atlas.setProgressCallback(msg -> System.out.println("[progress] " + msg));
		atlas.setErrorCallback(msg -> System.err.println("[error] " + msg));

		System.out.println("Initializing atlas (first run may download data)...");
		atlas.initialize(null, null);
		System.out.println("Atlas initialized.\n");

		// --- Check Atlas ---
		System.out.println("Atlas name: " + atlas.getName());
		System.out.println("Atlas URL: " + atlas.getURL());
		System.out.println("Atlas DOIs: " + atlas.getDOIs());
		assert atlas.getName().contains("example_mouse") : "Unexpected name";

		// --- Check Ontology ---
		AtlasOntology ontology = atlas.getOntology();
		assert ontology != null : "Ontology is null";

		AtlasNode root = ontology.getRoot();
		System.out.println("\nOntology root: id=" + root.getId()
				+ " name=" + root.data().get("name")
				+ " children=" + root.children().size());
		assert root.children().size() > 0 : "Root has no children";

		// Test node lookup
		AtlasNode rootLookup = ontology.getNodeFromId(root.getId());
		assert rootLookup != null : "Root lookup failed";
		assert rootLookup.getId().equals(root.getId()) : "Root lookup returned wrong node";
		System.out.println("Ontology naming property: " + ontology.getNamingProperty());

		// --- Check Map ---
		AtlasMap map = atlas.getMap();
		assert map != null : "Map is null";

		System.out.println("\nMap precision: " + map.getAtlasPrecisionInMillimeter() + " mm");
		System.out.println("Image keys: " + map.getImagesKeys());
		System.out.println("Label left=" + map.labelLeft() + " right=" + map.labelRight());

		// Check structural images exist
		assert map.getStructuralImages().containsKey("reference") : "Missing reference image";
		assert map.getStructuralImages().containsKey(AtlasHelper.KEY_BORDERS) : "Missing borders image";
		assert map.getStructuralImages().containsKey(AtlasHelper.KEY_X) : "Missing X coordinate image";
		assert map.getStructuralImages().containsKey(AtlasHelper.KEY_Y) : "Missing Y coordinate image";
		assert map.getStructuralImages().containsKey(AtlasHelper.KEY_Z) : "Missing Z coordinate image";
		assert map.getStructuralImages().containsKey(AtlasHelper.KEY_LEFT_RIGHT) : "Missing Left Right image";

		// Check label image
		assert map.getLabelImage() != null : "Label image is null";

		// Check image maxima
		Double refMax = map.getImageMax("reference");
		System.out.println("Reference image max: " + refMax);
		assert refMax > 0 : "Reference max should be positive";

		System.out.println("\n=== All tests passed! ===");
	}
}
