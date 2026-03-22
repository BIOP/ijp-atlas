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
package ch.epfl.biop.atlas.brainglobe;

import org.apposed.appose.Appose;
import org.apposed.appose.BuildException;
import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apposed.appose.builder.PixiBuilder;
import org.scijava.Context;
import org.scijava.task.TaskService;

/**
 * Handles Appose-based communication with the BrainGlobe Atlas API in Python.
 * <p>
 * This class manages the pixi environment creation and runs Python scripts
 * to list available atlases and fetch atlas data (file paths + metadata).
 */
public class BrainGlobeAppose {

	/** Default BrainGlobe Atlas API version */
	public static final String DEFAULT_BG_VERSION = "2.3.0";

	// --- Static session-level cache for atlas listing ---

	private enum ListingState { NOT_TRIED, SUCCEEDED, FAILED }
	private static volatile ListingState listingState = ListingState.NOT_TRIED;
	private static List<String> cachedAtlasNames = null;

	/**
	 * Returns the list of available BrainGlobe atlas names, with session-level caching.
	 * <ul>
	 *   <li>First call: builds the Python environment (slow) and fetches the list.</li>
	 *   <li>Subsequent calls: returns the cached list instantly.</li>
	 *   <li>If the first call fails (no mamba/network/etc.), returns an empty list
	 *       and will NOT retry for the remainder of this session.</li>
	 * </ul>
	 *
	 * @return list of atlas names, or empty list if unavailable
	 */
	public static synchronized List<String> getAvailableAtlasNames() {
		if (listingState == ListingState.SUCCEEDED) {
			return cachedAtlasNames;
		}
		if (listingState == ListingState.FAILED) {
			return Collections.emptyList();
		}

		// First attempt
		try {
			BrainGlobeAppose bg = new BrainGlobeAppose();
			cachedAtlasNames = bg.listAvailableAtlases();
			listingState = ListingState.SUCCEEDED;
			return cachedAtlasNames;
		} catch (Exception e) {
			System.err.println("BrainGlobe atlas listing failed (will not retry this session): " + e.getMessage());
			listingState = ListingState.FAILED;
			return Collections.emptyList();
		}
	}

	/**
	 * @return true if BrainGlobe atlases were successfully listed and are available
	 */
	public static boolean isBrainGlobeAvailable() {
		return listingState == ListingState.SUCCEEDED;
	}

	private final String bgVersion;
	private Environment environment;

	// Callbacks for environment build progress
	private Consumer<String> progressCallback;
	private Consumer<String> errorCallback;

	public BrainGlobeAppose() {
		this(DEFAULT_BG_VERSION);
	}

	private static Context ctx; // Used for monitoring download time

	public static void setContext(Context ctx) {
		BrainGlobeAppose.ctx = ctx;
	}

	public BrainGlobeAppose(String bgVersion) {
		this.bgVersion = bgVersion;
	}

	public void setProgressCallback(Consumer<String> callback) {
		this.progressCallback = callback;
	}

	public void setErrorCallback(Consumer<String> callback) {
		this.errorCallback = callback;
	}

	/**
	 * Creates or retrieves the mamba environment with brainglobe-atlasapi.
	 * The first call downloads and installs dependencies (slow).
	 * Subsequent calls reuse the existing environment (fast).
	 */
	public Environment getOrCreateEnvironment() throws BuildException {
		if (environment != null) {
			return environment;
		}

		PixiBuilder builder = Appose
				.pixi()
				.channels("conda-forge")
				.conda("python=3.11", "appose")
				.pypi("brainglobe-atlasapi==" + bgVersion)
				.name("brainglobe-abba-" + bgVersion)   // bump name to force rebuild after psutil addition
				.logDebug();

		if (progressCallback != null) {
			builder.subscribeProgress((msg, cur, max) -> progressCallback.accept(msg));
			builder.subscribeOutput(progressCallback);
		}
		if (errorCallback != null) {
			builder.subscribeError(errorCallback);
		}

		environment = builder.build();
		return environment;
	}

	/**
	 * Lists all available BrainGlobe atlases (name and latest version).
	 *
	 * @return list of atlas names (e.g. "allen_mouse_25um", "kim_unified_25um")
	 */
	public List<String> listAvailableAtlases() throws Exception {
		Environment env = getOrCreateEnvironment();
		try (Service python = env.python().init(
				"from brainglobe_atlasapi import show_atlases\n"
					+ "from brainglobe_atlasapi.list_atlases import get_all_atlases_lastversions\n"
		)) {
			Task task = python.task(listAtlasesScript());
			task.start();
			task.waitFor();

			if (task.status != TaskStatus.COMPLETE) {
				throw new RuntimeException("Failed to list atlases: " + task.error);
			}

			String json = (String) task.outputs.get("atlases");
			return new Gson().fromJson(json, new TypeToken<List<String>>(){}.getType());
		}
	}

	/**
	 * Fetches a BrainGlobe atlas by name. Downloads the atlas if not cached locally.
	 * Returns all image data as shared memory NDArrays plus metadata.
	 *
	 * @param atlasName the BrainGlobe atlas name (e.g. "example_mouse_100um")
	 * @return atlas data containing NDArrays and metadata
	 */
	public BrainGlobeAtlasData fetchAtlas(String atlasName) throws Exception {
		Environment env = getOrCreateEnvironment();
		try (Service python = env.python().init(
				"from brainglobe_atlasapi import BrainGlobeAtlas\n"
				+ "import json\n"
				+ "import pathlib\n"
		)) {
			String script = fetchAtlasScript(atlasName);

			org.scijava.task.Task fetchAtlasTask;
			if (ctx !=null) {
				fetchAtlasTask = ctx.getService(TaskService.class).createTask("Fetching Atlas " + atlasName);
			} else {
				fetchAtlasTask = null;
			}

			try {
				Task task = python
						.task(script)
						.listen(event -> {
							switch (event.responseType) {
								case LAUNCH:
									if (fetchAtlasTask!=null) {
										fetchAtlasTask.start();
									}
								case UPDATE:
									if (fetchAtlasTask.getProgressMaximum() <= 0) {
										fetchAtlasTask.setProgressMaximum(event.maximum);
									}
									if (fetchAtlasTask!=null) {
										fetchAtlasTask.setProgressValue(event.current);
									}
									break;
								case FAILURE:
									if (errorCallback != null) {
										errorCallback.accept("Atlas fetch failed. ");
									}
									if (fetchAtlasTask!=null) {
										fetchAtlasTask.setStatusMessage("Atlas fetch failed");
									}
									break;
								default:
									break;
							}
						});

				task.start();
				task.waitFor();

				if (task.status != TaskStatus.COMPLETE) {
					throw new RuntimeException("Failed to fetch atlas '" + atlasName + "': " + task.error);
				}

				return extractAtlasData(task);
			} finally {
				if (fetchAtlasTask!=null) {
					fetchAtlasTask.finish();
				}
			}
		}
	}

	// --- Data container ---

	/**
	 * Holds the data returned from the Python BrainGlobe fetch.
	 * Image data is referenced by file paths (TIFF files on disk)
	 * rather than shared memory, avoiding memory issues with large atlases.
	 */
	public static class BrainGlobeAtlasData {
		/** Atlas metadata as a map (resolution, orientation, name, etc.) */
		public final Map<String, Object> metadata;
		/** File path to the reference image (TIFF) */
		public final String referencePath;
		/** File path to the annotation/label image (TIFF) */
		public final String annotationPath;
		/** File path to the hemispheres image (TIFF) */
		public final String hemispheresPath;
		/** File paths to additional reference channels, keyed by name */
		public final Map<String, String> additionalReferencePaths;
		/** Raw structures JSON content for ontology building */
		public final String structuresJson;

		public BrainGlobeAtlasData(Map<String, Object> metadata,
								   String referencePath,
								   String annotationPath,
								   String hemispheresPath,
								   Map<String, String> additionalReferencePaths,
								   String structuresJson) {
			this.metadata = metadata;
			this.referencePath = referencePath;
			this.annotationPath = annotationPath;
			this.hemispheresPath = hemispheresPath;
			this.additionalReferencePaths = additionalReferencePaths;
			this.structuresJson = structuresJson;
		}

		/** Resolution in micrometers [z, y, x] */
		@SuppressWarnings("unchecked")
		public List<Double> getResolution() {
			List<Number> raw = (List<Number>) metadata.get("resolution");
			List<Double> result = new ArrayList<>();
			for (Number n : raw) result.add(n.doubleValue());
			return result;
		}

		/** Atlas name */
		public String getAtlasName() {
			return (String) metadata.get("atlas_name");
		}

		/** Orientation string (e.g. "asr") */
		public String getOrientation() {
			return (String) metadata.get("orientation");
		}

		/** Citation string */
		public String getCitation() {
			return (String) metadata.get("citation");
		}

		/** Atlas link URL */
		public String getAtlasLink() {
			return (String) metadata.get("atlas_link");
		}

		/** Whether the atlas uses a symmetric reference (no hemispheres.tiff) */
		public boolean isSymmetric() {
			Boolean sym = (Boolean) metadata.get("symmetric");
			return sym != null && sym;
		}

		/** Index of the frontal (left-right) axis in the shape array (0, 1, or 2) */
		public int getFrontalAxisIndex() {
			Number idx = (Number) metadata.get("frontal_axis_index");
			return idx != null ? idx.intValue() : 2;
		}

		/** Shape of the reference volume [dim0, dim1, dim2] */
		@SuppressWarnings("unchecked")
		public long[] getShape() {
			List<Number> raw = (List<Number>) metadata.get("shape");
			long[] shape = new long[raw.size()];
			for (int i = 0; i < raw.size(); i++) shape[i] = raw.get(i).longValue();
			return shape;
		}

		/** Names of additional reference channels */
		@SuppressWarnings("unchecked")
		public List<String> getAdditionalReferenceNames() {
			List<String> names = (List<String>) metadata.get("additional_references");
			return names != null ? names : new ArrayList<>();
		}
	}

	// --- Private helpers ---

	private BrainGlobeAtlasData extractAtlasData(Task task) {
		String metadataJson = (String) task.outputs.get("metadata");
		Map<String, Object> metadata = new Gson().fromJson(metadataJson,
				new TypeToken<Map<String, Object>>(){}.getType());

		String referencePath = (String) task.outputs.get("reference_path");
		String annotationPath = (String) task.outputs.get("annotation_path");
		String hemispheresPath = (String) task.outputs.get("hemispheres_path");
		String structuresJson = (String) task.outputs.get("structures_json");

		@SuppressWarnings("unchecked")
		List<String> additionalRefNames = (List<String>) metadata.get("additional_references");
		Map<String, String> additionalRefPaths = new HashMap<>();
		if (additionalRefNames != null) {
			for (int i = 0; i < additionalRefNames.size(); i++) {
				String path = (String) task.outputs.get("additional_ref_path_" + i);
				if (path != null) {
					additionalRefPaths.put(additionalRefNames.get(i), path);
				}
			}
		}

		return new BrainGlobeAtlasData(metadata, referencePath, annotationPath, hemispheresPath,
				additionalRefPaths, structuresJson);
	}

	private String listAtlasesScript() {
		return //"from brainglobe_atlasapi import show_atlases\n"
				//+ "from brainglobe_atlasapi.list_atlases import get_all_atlases_lastversions\n"
				/*+*/ "import json\n"
				+ "\n"
				+ "atlases = list(get_all_atlases_lastversions().keys())\n"
				+ "task.outputs['atlases'] = json.dumps(atlases)\n";
	}

	private String fetchAtlasScript(String atlasName) {
		// Sanitize the atlas name to prevent injection
		String safeName = atlasName.replace("'", "").replace("\\", "").replace("\n", "");
		return "atlas_name = '" + safeName + "'\n"
				+ "\n"
				+ "# Progress callback for atlas download\n"
				+ "def download_progress(completed, total):\n"
				+ "    if total > 0:\n"
				+ "        task.update('Downloading ' + atlas_name + ': ' + str(int(100 * completed / total)) + '%', completed, total)\n"
				+ "    else:\n"
				+ "        task.update('Downloading ' + atlas_name + '...', 0, 0)\n"
				+ "\n"
				+ "task.update('Downloading...')\n"
				+ "atlas = BrainGlobeAtlas(atlas_name, fn_update=download_progress)\n"
				+ "\n"
				+ "root = pathlib.Path(atlas.root_dir)\n"
				+ "\n"
				+ "# Collect metadata\n"
				+ "metadata = {\n"
				+ "    'atlas_name': atlas.atlas_name,\n"
				+ "    'resolution': [float(r) for r in atlas.metadata['resolution']],\n"
				+ "    'orientation': atlas.orientation,\n"
				+ "    'symmetric': bool(atlas.metadata.get('symmetric', False)),\n"
				+ "    'frontal_axis_index': atlas.space.axes_order.index('frontal'),\n"
				+ "    'citation': atlas.metadata.get('citation', ''),\n"
				+ "    'atlas_link': atlas.metadata.get('atlas_link', ''),\n"
				+ "    'additional_references': list(atlas.metadata.get('additional_references', [])),\n"
				+ "    'shape': [int(s) for s in atlas.shape],\n"
				+ "}\n"
				+ "task.outputs['metadata'] = json.dumps(metadata)\n"
				+ "\n"
				+ "# Read structures.json content\n"
				+ "structures_path = root / 'structures.json'\n"
				+ "task.outputs['structures_json'] = structures_path.read_text()\n"
				+ "\n"
				+ "# Return file paths instead of shared memory arrays\n"
				+ "task.outputs['reference_path'] = str(root / 'reference.tiff')\n"
				+ "task.outputs['annotation_path'] = str(root / 'annotation.tiff')\n"
				+ "task.outputs['hemispheres_path'] = str(root / 'hemispheres.tiff')\n"
				+ "\n"
				+ "# Additional reference channels\n"
				+ "for i, name in enumerate(atlas.metadata.get('additional_references', [])):\n"
				+ "    task.outputs['additional_ref_path_' + str(i)] = str(root / name + '.tiff')\n";
	}
}
