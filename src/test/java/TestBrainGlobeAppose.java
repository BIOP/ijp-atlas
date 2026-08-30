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
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAppose;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAppose.BrainGlobeAtlasData;
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

	public static void main(String[] args) throws Exception {
		BrainGlobeAppose bg = new BrainGlobeAppose();
		bg.setProgressCallback(msg -> System.out.println("[progress] " + msg));
		bg.setErrorCallback(msg -> System.err.println("[error] " + msg));

		// --- Test 1: List available atlases ---
		System.out.println("=== Test 1: Listing available atlases ===");
		List<String> atlases = bg.listAvailableAtlases();
		System.out.println("Found " + atlases.size() + " atlases:");
		for (String name : atlases) {
			System.out.println("  - " + name);
		}
		assert !atlases.isEmpty() : "Expected at least one atlas";
		System.out.println("PASSED\n");

		// --- Test 2: Fetch a small test atlas ---
		System.out.println("=== Test 2: Fetching example_mouse_100um ===");
		BrainGlobeAtlasData data = bg.fetchAtlas("example_mouse_100um");

		System.out.println("Atlas name: " + data.getAtlasName());
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
