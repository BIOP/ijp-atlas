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
import ch.epfl.biop.atlas.AtlasLocationHelper;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAtlas;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;

/**
 * Manual test for BrainGlobeAtlas full pipeline.
 * <p>
 * Tests the complete flow: Appose Python fetch -> AtlasMap (BDV sources) + AtlasOntology.
 * Defaults to "example_mouse_100um@3.0", a small test atlas.
 * <p>
 * Run with: mvn exec:java -Dexec.mainClass="TestBrainGlobeAtlas" -Dexec.classpathScope=test
 * An atlas id (name@version) may be passed as the first argument.
 */
public class TestBrainGlobeAtlas {

	static {
		// The SciJava context below pulls in the ImageJ legacy service, which has to
		// patch ij.IJ before that class gets loaded by anything else
		net.imagej.patcher.LegacyInjector.preinit();
	}

	public static void main(String[] args) throws Exception {

		System.out.println("=== Test: Full BrainGlobeAtlas pipeline ===\n");

		// The map reads its TIFFs through SCIFIO, which needs a SciJava context
		AtlasLocationHelper.setContext(new org.scijava.Context());

		// Create and initialize the atlas. Ids carry a version; a bare name is refused
		String atlasName = args.length > 0 ? args[0] : "example_mouse_100um@3.0";
		BrainGlobeAtlas atlas = new BrainGlobeAtlas(atlasName);
		atlas.setProgressCallback(msg -> System.out.println("[progress] " + msg));
		atlas.setErrorCallback(msg -> System.err.println("[error] " + msg));

		System.out.println("Initializing atlas (first run may download data)...");
		atlas.initialize(null, null);
		System.out.println("Atlas initialized.\n");

		// --- Check Atlas ---
		System.out.println("Atlas name: " + atlas.getName());
		System.out.println("Atlas URL: " + atlas.getURL());
		System.out.println("Atlas DOIs: " + atlas.getDOIs());
		assert atlas.getName().equals(atlasName) : "Unexpected name";

		// --- Check Ontology ---
		AtlasOntology ontology = atlas.getOntology();
		assert ontology != null : "Ontology is null";

		AtlasNode root = ontology.getRoot();
		System.out.println("\nOntology root: id=" + root.getId()
				+ " name=" + root.data().get("name")
				+ " children=" + root.children().size());
		assert !root.children().isEmpty() : "Root has no children";

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
		System.out.println("Channel colours:");
		for (String key : map.getImagesKeys()) {
			Object converter = map.getStructuralImages().get(key).getConverter();
			String color = converter instanceof net.imglib2.display.ColorConverter
					? String.format("#%06X", ((net.imglib2.display.ColorConverter) converter).getColor().get() & 0xFFFFFF)
					: "n/a";
			System.out.println("  " + color + "  " + key);
		}
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
		// The SciJava context keeps non-daemon threads alive and one of its shutdown
		// hooks never returns, so bypass both rather than hanging the test
		Runtime.getRuntime().halt(0);
	}
}
