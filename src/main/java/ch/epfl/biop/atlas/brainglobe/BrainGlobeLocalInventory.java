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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tells which BrainGlobe atlases are already usable on this machine, by looking
 * at the filesystem and nothing else — no Python, no Pixi environment, no network.
 * <p>
 * This exists so the atlas chooser can list what is installed before, and
 * independently of, anything that can fail. A machine that has never been online
 * still shows every atlas it has.
 * <p>
 * <b>"Downloaded" is not the question worth asking.</b> Since brainglobe-atlasapi
 * 3.x, downloading an atlas fetches a {@code manifest.json} plus metadata while the
 * volumes stay as remote OME-Zarr, fetched chunk by chunk on demand — and
 * {@code list_atlases.get_downloaded_atlases()} reports that state as downloaded.
 * ABBA deliberately does not stream (see {@code BrainGlobeAppose#fetchAtlasScript}):
 * an atlas is materialized in full as plain TIFFs before use. So the only state that
 * means "ready to open, instantly, offline" is the presence of those TIFFs, which is
 * what this class looks for:
 *
 * <pre>
 * &lt;brainglobe_dir&gt;/brainglobe-atlasapi/atlases/&lt;name&gt;/&lt;version&gt;/reference.tiff
 *                                                             /annotation.tiff
 *                                                             /hemispheres.tiff  (asymmetric atlases only)
 * </pre>
 *
 * The scan is a few hundred {@code File.exists()} calls and takes well under a
 * millisecond, so it is deliberately <b>not</b> cached: it is re-run every time the
 * chooser is opened, which is what makes an atlas downloaded a moment ago show up as
 * installed the next time round, with no cache to invalidate.
 */
public class BrainGlobeLocalInventory {

	/** Written by the Python side; see {@code BrainGlobeAppose#fetchAtlasScript} */
	public static final String REFERENCE_TIFF = "reference.tiff";
	public static final String ANNOTATION_TIFF = "annotation.tiff";
	public static final String HEMISPHERES_TIFF = "hemispheres.tiff";

	private BrainGlobeLocalInventory() {}

	/**
	 * Locates the BrainGlobe store, mirroring {@code brainglobe_atlasapi.config}:
	 * the {@code brainglobe_dir} entry of section {@code default_dirs} in
	 * {@code bg_config.conf}, which lives in {@code $BRAINGLOBE_CONFIG_DIR} or, by
	 * default, {@code ~/.config/brainglobe}. Falls back to {@code ~/.brainglobe},
	 * which is what upstream writes into a freshly created config anyway.
	 *
	 * @return the configured BrainGlobe directory; may not exist
	 */
	public static File getBrainGlobeDir() {
		File configured = readConfiguredDir();
		if (configured != null) {
			return configured;
		}
		return new File(System.getProperty("user.home"), ".brainglobe");
	}

	/** @return the {@code atlases} folder holding one subfolder per atlas name */
	public static File getAtlasesDir() {
		return new File(new File(getBrainGlobeDir(), "brainglobe-atlasapi"), "atlases");
	}

	/** @return the versioned folder of a given atlas, whether or not it exists */
	public static File getAtlasDir(BrainGlobeAtlasId id) {
		return new File(new File(getAtlasesDir(), id.getName()), id.getVersionFolder());
	}

	/**
	 * Tells whether an atlas has been materialized as TIFFs and can therefore be
	 * opened without network access.
	 * <p>
	 * {@value #HEMISPHERES_TIFF} is not required: a symmetric atlas never has one,
	 * its hemispheres being derived from the volume shape instead.
	 */
	public static boolean isMaterialized(BrainGlobeAtlasId id) {
		File dir = getAtlasDir(id);
		return new File(dir, REFERENCE_TIFF).exists() && new File(dir, ANNOTATION_TIFF).exists();
	}

	/**
	 * Scans the store for every materialized atlas version.
	 * <p>
	 * Several versions of one atlas may be installed side by side, and each is
	 * listed separately — that is the point of versioned ids. Never throws: an
	 * unreadable or absent store simply yields an empty list, so a broken BrainGlobe
	 * installation degrades to "no BrainGlobe atlases" rather than to a failure.
	 *
	 * @return materialized atlas ids, sorted by name then version
	 */
	public static List<BrainGlobeAtlasId> listMaterialized() {
		List<BrainGlobeAtlasId> found = new ArrayList<>();
		File atlasesDir = getAtlasesDir();
		File[] atlasFolders = atlasesDir.listFiles(File::isDirectory);
		if (atlasFolders == null) {
			return found; // store absent or unreadable
		}
		for (File atlasFolder : atlasFolders) {
			File[] versionFolders = atlasFolder.listFiles(File::isDirectory);
			if (versionFolders == null) continue;
			for (File versionFolder : versionFolders) {
				BrainGlobeAtlasId id;
				try {
					id = BrainGlobeAtlasId.of(atlasFolder.getName(),
							versionFolder.getName().replace('_', '.'));
				} catch (IllegalArgumentException e) {
					continue; // not a version folder we understand
				}
				if (isMaterialized(id)) {
					found.add(id);
				}
			}
		}
		found.sort(Comparator.comparing(BrainGlobeAtlasId::getName)
				.thenComparing(BrainGlobeAtlasId::getVersion));
		return found;
	}

	/**
	 * Reads {@code brainglobe_dir} out of {@code bg_config.conf}.
	 * <p>
	 * A hand-rolled two-key INI read rather than a dependency: the file has one
	 * section and two entries, and any trouble reading it just means falling back to
	 * the default location.
	 *
	 * @return the configured directory, or null if unset or unreadable
	 */
	private static File readConfiguredDir() {
		String configDir = System.getenv("BRAINGLOBE_CONFIG_DIR");
		File configFile = configDir != null && !configDir.isEmpty()
				? new File(configDir, "bg_config.conf")
				: new File(new File(new File(System.getProperty("user.home"), ".config"), "brainglobe"),
						"bg_config.conf");
		if (!configFile.isFile()) {
			return null;
		}
		try {
			boolean inDefaultDirs = false;
			for (String rawLine : Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8)) {
				String line = rawLine.trim();
				if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue;
				if (line.startsWith("[")) {
					inDefaultDirs = line.equals("[default_dirs]");
					continue;
				}
				if (!inDefaultDirs) continue;
				int eq = line.indexOf('=');
				if (eq < 0) continue;
				if (!line.substring(0, eq).trim().equals("brainglobe_dir")) continue;
				String value = line.substring(eq + 1).trim();
				return value.isEmpty() ? null : new File(value);
			}
		} catch (IOException | RuntimeException e) {
			System.err.println("Could not read BrainGlobe config at " + configFile
					+ ", falling back to the default location: " + e.getMessage());
		}
		return null;
	}
}
