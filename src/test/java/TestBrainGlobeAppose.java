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
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAppose;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAppose.BrainGlobeAtlasData;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAtlasId;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeHelper;
import ch.epfl.biop.atlas.struct.AtlasNode;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

/**
 * Manual test for BrainGlobeAppose.
 * <p>
 * Requires network access and mamba. First run will be slow (environment creation).
 * Uses "example_mouse_100um" which is a small test atlas (~2 MB).
 * <p>
 * Run with: mvn exec:java -Dexec.mainClass="TestBrainGlobeAppose"
 */
public class TestBrainGlobeAppose {

	/** Small test atlas (~2 MB), kept small on purpose */
	private static final String EXAMPLE_ATLAS = "example_mouse_100um";

	public static void main(String[] args) throws Exception {
		BrainGlobeAppose bg = new BrainGlobeAppose();
		bg.setProgressCallback(msg -> System.out.println("[progress] " + msg));
		bg.setErrorCallback(msg -> System.err.println("[error] " + msg));

		// --- Test 1: List available atlases ---
		System.out.println("=== Test 1: Listing available atlases ===");
		List<BrainGlobeAtlasId> atlases = bg.listAvailableAtlases();
		System.out.println("Found " + atlases.size() + " atlases:");
		for (BrainGlobeAtlasId id : atlases) {
			System.out.println("  - " + id);
		}
		assert !atlases.isEmpty() : "Expected at least one atlas";
		System.out.println("PASSED\n");

		// --- Test 2: Fetch a small test atlas ---
		// The version comes from the catalogue rather than being hard-coded, so this
		// keeps testing the latest example atlas as it is republished
		BrainGlobeAtlasId exampleId = atlases.stream()
				.filter(id -> id.getName().equals(EXAMPLE_ATLAS))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						EXAMPLE_ATLAS + " is not in the BrainGlobe catalogue"));

		System.out.println("=== Test 2: Fetching " + exampleId + " ===");
		BrainGlobeAtlasData data = bg.fetchAtlas(exampleId);

		assert exampleId.equals(data.getId()) : "Fetched " + data.getId() + " instead of " + exampleId;
		System.out.println("Atlas name: " + data.getAtlasName());
		System.out.println("Atlas version: " + data.getVersion());
		System.out.println("Orientation: " + data.getOrientation());
		System.out.println("Resolution (um): " + data.getResolution());
		System.out.println("Reference path: " + data.referencePath);
		System.out.println("Annotation path: " + data.annotationPath);
		System.out.println("Hemispheres path: " + data.hemispheresPath);
		System.out.println("Additional references: " + data.getAdditionalReferenceNames());
		System.out.println("Additional reference paths: " + data.additionalReferencePaths);
		System.out.println("Structures JSON length: " + data.structuresJson.length() + " chars");

		// Basic assertions
		assert data.getAtlasName().contains("example_mouse") : "Unexpected atlas name: " + data.getAtlasName();
		assert data.getOrientation().equals("asr") : "Expected 'asr' orientation, got: " + data.getOrientation();
		assert data.referencePath != null : "Reference path is null";
		assert data.annotationPath != null : "Annotation path is null";
		assert data.hemispheresPath != null : "Hemispheres path is null";
		assert new File(data.referencePath).exists() : "Reference file does not exist: " + data.referencePath;
		assert new File(data.annotationPath).exists() : "Annotation file does not exist: " + data.annotationPath;
		// Symmetric atlases ship no hemispheres image: it is derived from the shape instead
		assert data.isSymmetric() || new File(data.hemispheresPath).exists()
				: "Hemispheres file does not exist: " + data.hemispheresPath;
		assert data.structuresJson != null && !data.structuresJson.isEmpty() : "Structures JSON is empty";

		System.out.println("PASSED\n");

		// --- Test 3: Parse structures JSON with BrainGlobeHelper ---
		System.out.println("=== Test 3: Parsing ontology from structures JSON ===");

		// Write structures JSON to a temp file (BrainGlobeHelper reads from file)
		File tempStructures = File.createTempFile("structures_", ".json");
		tempStructures.deleteOnExit();
		try (FileWriter fw = new FileWriter(tempStructures)) {
			fw.write(data.structuresJson);
		}

		AtlasNode root = BrainGlobeHelper.buildTreeAndGetRoot(tempStructures.getAbsolutePath());
		System.out.println("Root node id: " + root.getId());
		System.out.println("Root node name: " + root.data().get("name"));
		System.out.println("Root has " + root.children().size() + " children");

		assert root.getId() != null : "Root id is null";
		assert !root.children().isEmpty() : "Root has no children";

		System.out.println("PASSED\n");

		System.out.println("=== All tests passed! ===");
	}
}
