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

import ij.IJ;
import org.apposed.appose.Appose;
import org.apposed.appose.BuildException;
import org.apposed.appose.Environment;
import org.apposed.appose.Service;
import org.apposed.appose.Service.Task;
import org.apposed.appose.Service.TaskStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apposed.appose.builder.PixiBuilder;
import ch.epfl.biop.atlas.AtlasLocationHelper;
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
	public static final String BG_VERSION = "3.0.1";

	// --- Static session-level cache for atlas listing ---

	private enum ListingState { NOT_TRIED, SUCCEEDED, FAILED }
	private static volatile ListingState listingState = ListingState.NOT_TRIED;
	private static List<BrainGlobeAtlasId> cachedAtlasIds = null;

	/**
	 * Returns the latest published version of every BrainGlobe atlas, with
	 * session-level caching.
	 * <ul>
	 *   <li>First call: builds the Python environment (slow) and fetches the list.</li>
	 *   <li>Subsequent calls: returns the cached list instantly.</li>
	 *   <li>If the first call fails (no pixi/network/etc.), returns an empty list
	 *       and will NOT retry for the remainder of this session.</li>
	 * </ul>
	 * This is the <i>remote catalogue</i>, and every part of it can fail. What is
	 * already installed comes from {@link BrainGlobeLocalInventory} instead, which
	 * cannot.
	 *
	 * @return latest version of each atlas, or an empty list if unavailable
	 */
	public static synchronized List<BrainGlobeAtlasId> getAvailableAtlasIds() {
		if (listingState == ListingState.SUCCEEDED) {
			return cachedAtlasIds;
		}
		if (listingState == ListingState.FAILED) {
			return Collections.emptyList();
		}

		// First attempt
		try {
			BrainGlobeAppose bg = new BrainGlobeAppose();
			cachedAtlasIds = bg.listAvailableAtlases();
			listingState = ListingState.SUCCEEDED;
			return cachedAtlasIds;
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
		this.bgVersion = BG_VERSION;
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
				.name("brainglobe-abba-" + bgVersion)   // environment is keyed by the brainglobe-atlasapi version
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
	 * Lists all published BrainGlobe atlases with their latest version.
	 *
	 * @return one id per atlas, e.g. {@code allen_mouse_25um@3.1}
	 */
	public List<BrainGlobeAtlasId> listAvailableAtlases() throws Exception {
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
			Map<String, String> latestVersions = new Gson().fromJson(json,
					new TypeToken<LinkedHashMap<String, String>>(){}.getType());

			List<BrainGlobeAtlasId> ids = new ArrayList<>();
			for (Map.Entry<String, String> entry : latestVersions.entrySet()) {
				try {
					ids.add(BrainGlobeAtlasId.of(entry.getKey(), entry.getValue()));
				} catch (IllegalArgumentException e) {
					// One malformed catalogue entry must not cost us the other 221
					System.err.println("Skipping BrainGlobe atlas '" + entry.getKey() + "': " + e.getMessage());
				}
			}
			return ids;
		}
	}

	/**
	 * Fetches a BrainGlobe atlas at a specific version, downloading and materializing
	 * it if it is not already on disk. Returns image data as cached TIFF file paths
	 * plus metadata.
	 * <p>
	 * The version is always pinned, so an atlas published in a newer version after a
	 * dataset was aligned still opens as the version it was aligned against.
	 *
	 * @param id the versioned atlas id, e.g. {@code example_mouse_100um@3.1}
	 * @return atlas data containing TIFF paths and metadata
	 */
	public BrainGlobeAtlasData fetchAtlas(BrainGlobeAtlasId id) throws Exception {
		String atlasName = id.getName();
		Environment env = getOrCreateEnvironment();
		try (Service python = env.python().init(
				"from brainglobe_atlasapi import BrainGlobeAtlas\n"
				+ "from brainglobe_atlasapi import bg_atlas as bg_atlas_module\n"
				+ "from brainglobe_atlasapi import core as bg_core_module\n"
				+ "from fsspec.callbacks import Callback\n"
				+ "import json\n"
				+ "import pathlib\n"
				+ "import re\n"
				+ "import tifffile\n"
		)) {
			String script = fetchAtlasScript(id);

			org.scijava.task.Task fetchAtlasTask;
			Context ctx = AtlasLocationHelper.getContext();
			if (ctx != null) {
				fetchAtlasTask = ctx.getService(TaskService.class).createTask("Fetching Atlas " + id);
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
										IJ.showStatus(fetchAtlasTask.getStatusMessage());
									}
									break;
								case UPDATE:
									if (fetchAtlasTask!=null) {
										// A single fetch chains several downloads (manifest, terminology,
										// template, annotation, hemispheres, additional references), each
										// with its own total, so the maximum is refreshed on every event.
										fetchAtlasTask.setProgressMaximum(event.maximum);
										fetchAtlasTask.setProgressValue(event.current);
										if (event.message != null) {
											fetchAtlasTask.setStatusMessage(event.message);
											IJ.showStatus(event.message);
										} else {
											IJ.showStatus("Loading "+atlasName);
										}
										if (event.maximum > 0) {
											IJ.showProgress((double) event.current / event.maximum);
										}
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
					throw new RuntimeException("Failed to fetch atlas '" + id + "': " + task.error);
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

		/** Bare atlas name, without a version */
		public String getAtlasName() {
			return (String) metadata.get("atlas_name");
		}

		/** Dotted version of the atlas that was actually opened, e.g. {@code 3.1} */
		public String getVersion() {
			return (String) metadata.get("version");
		}

		/** @return the versioned id, e.g. {@code allen_mouse_50um@3.1} */
		public BrainGlobeAtlasId getId() {
			return BrainGlobeAtlasId.of(getAtlasName(), getVersion());
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
		// Linked map: the channel order must follow the manifest, not hash order
		Map<String, String> additionalRefPaths = new LinkedHashMap<>();
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
		// The name -> latest version mapping is handed over as-is; Java owns the
		// name@version convention, which does not exist upstream.
		return "import json\n"
				+ "\n"
				+ "atlases = {str(k): str(v) for k, v in get_all_atlases_lastversions().items()}\n"
				+ "task.outputs['atlases'] = json.dumps(atlases)\n";
	}

	/**
	 * Builds the Python script that downloads an atlas and hands its data back as
	 * file paths plus JSON metadata.
	 * <p>
	 * brainglobe-atlasapi 3.x no longer ships an atlas as a self-contained folder of
	 * TIFFs: an atlas is a manifest referencing separately versioned components
	 * (template, annotation set, terminology, coordinate space), the images are
	 * remote OME-Zarr pyramids fetched chunk by chunk, and the ontology lives in a
	 * CSV rather than a {@code structures.json}. The script therefore materializes
	 * the full-resolution volumes once and caches them as plain TIFFs next to the
	 * manifest, reproducing the 2.x on-disk layout that the Java side reads lazily.
	 */
	private String fetchAtlasScript(BrainGlobeAtlasId id) {
		// Sanitize the atlas name to prevent injection. The version is already known
		// to be dot-separated integers, BrainGlobeAtlasId having rejected anything else.
		String safeName = id.getName().replace("'", "").replace("\\", "").replace("\n", "");
		return "atlas_name = '" + safeName + "'\n"
				+ "atlas_version = '" + id.getVersion() + "'\n" + """

				# brainglobe-atlasapi 3.x downloads OME-Zarr chunks through fsspec with a
				# hard-coded TqdmCallback; swapping the class out in both modules routes
				# that progress back to the Appose task.
				class ApposeCallback(Callback):
				    def __init__(self, *args, **kwargs):
				        super().__init__()

				    def call(self, hook_name=None, **kwargs):
				        task.update('Downloading ' + atlas_name + '...', self.value, self.size or 0)

				bg_core_module.TqdmCallback = ApposeCallback
				bg_atlas_module.TqdmCallback = ApposeCallback

				task.update('Downloading ' + atlas_name + '...')
				# version is pinned so that an atlas republished upstream never silently
				# takes the place of the one a dataset was aligned against; check_latest
				# is off so that opening an atlas already on disk touches no network at
				# all (it would otherwise cost an S3 listing just to print a hint).
				atlas = BrainGlobeAtlas(
				    atlas_name, version=atlas_version, check_latest=False
				)

				# In 3.x atlas.root_dir is the shared BrainGlobe store, not the atlas folder;
				# the atlas manifest lives under the location advertised in its metadata.
				root = pathlib.Path(atlas.root_dir) / atlas.metadata['location'].lstrip('/')

				# Additional references are component dicts in 3.x, plain names in 2.x
				additional_references = [
				    ref['name'] if isinstance(ref, dict) else str(ref)
				    for ref in atlas.metadata.get('additional_references', [])
				]

				metadata = {
				    'atlas_name': atlas.atlas_name,
				    'version': str(atlas.metadata.get('version', atlas_version)).replace('_', '.'),
				    'resolution': [float(r) for r in atlas.resolution],
				    'orientation': atlas.orientation,
				    'symmetric': bool(atlas.metadata.get('symmetric', False)),
				    'frontal_axis_index': atlas.space.axes_order.index('frontal'),
				    'citation': atlas.metadata.get('citation', ''),
				    'atlas_link': atlas.metadata.get('atlas_link', ''),
				    'additional_references': additional_references,
				    'shape': [int(s) for s in atlas.shape],
				}
				task.outputs['metadata'] = json.dumps(metadata)

				# The ontology comes from terminology.csv in 3.x; rebuild the structures.json
				# payload that BrainGlobeHelper parses.
				structures = [
				    {
				        'acronym': str(s['acronym']),
				        'id': int(s['id']),
				        'name': str(s['name']),
				        'structure_id_path': [int(i) for i in s['structure_id_path']],
				        'rgb_triplet': [int(c) for c in s['rgb_triplet']],
				    }
				    for s in atlas.structures_list
				]
				task.outputs['structures_json'] = json.dumps(structures)

				def cache_tiff(filename, produce):
				    # Materialize a volume once and keep it as a plain TIFF, so that
				    # re-opening the atlas costs nothing and Java can read it lazily.
				    path = root / filename
				    if not path.exists():
				        array = produce()
				        partial = path.with_name(path.name + '.part')
				        tifffile.imwrite(str(partial), array)
				        partial.replace(path)
				        del array
				    return str(path)

				# Each volume is held in memory only while it is being written out;
				# dropping the atlas' cache afterwards keeps a single volume resident.
				task.update('Loading reference of ' + atlas_name)
				task.outputs['reference_path'] = cache_tiff('reference.tiff', lambda: atlas.template)
				atlas._template = None

				task.update('Loading annotations of ' + atlas_name)
				task.outputs['annotation_path'] = cache_tiff('annotation.tiff', lambda: atlas.annotation)
				atlas._annotation = None

				if metadata['symmetric']:
				    # Java derives the hemispheres of a symmetric atlas from its shape
				    task.outputs['hemispheres_path'] = str(root / 'hemispheres.tiff')
				else:
				    task.update('Loading hemispheres of ' + atlas_name)
				    task.outputs['hemispheres_path'] = cache_tiff('hemispheres.tiff', lambda: atlas.hemispheres)
				    atlas._hemispheres = None

				for i, name in enumerate(additional_references):
				    task.update('Loading ' + name + ' of ' + atlas_name)
				    filename = re.sub(r'[^A-Za-z0-9._-]', '_', name) + '.tiff'
				    task.outputs['additional_ref_path_' + str(i)] = cache_tiff(
				        filename, lambda n=name: atlas.additional_references[n])
				    atlas.additional_references.data[name] = None
				""";
	}
}
