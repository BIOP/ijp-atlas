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

import java.util.regex.Pattern;

/**
 * A versioned BrainGlobe atlas identifier, written {@code name@version}, for
 * example {@code allen_mouse_50um@3.1}.
 * <p>
 * <b>This is our convention, not BrainGlobe's.</b> Upstream keeps the two apart
 * everywhere: {@code AtlasName} is a literal type of bare names, the version is a
 * separate {@code BrainGlobeAtlas(name, version=...)} argument, on disk they are
 * separate path segments, and {@code last_versions.conf} is an INI mapping of one
 * to the other. Nothing upstream ever accepts a combined string, so an id must be
 * split before it crosses into Python.
 * <p>
 * A single string is used on the Java side because the atlas identity has to
 * survive as one value through places that only carry a name: the atlas chooser
 * dropdown, its comma-separated list of additional atlases, {@code Atlas.getName()},
 * and the {@code ObjectService} lookup that reuses an already-open atlas — the
 * last of which would otherwise hand back a 3.0 atlas to someone asking for 3.1.
 * <p>
 * {@code @} is the separator because it is the only character that is safe: of the
 * 222 atlases currently published, none contains an {@code @}, while 14 contain a
 * dot ({@code kocher_bumblebee_2.542um}) and many contain hyphens
 * ({@code kim_dev_mouse_e15-5_mri-adc_37.5um}). Parsing therefore splits on the
 * last {@code @} and never on a dot.
 *
 * @see BrainGlobeLocalInventory
 */
public final class BrainGlobeAtlasId {

	/** Separator between the atlas name and its version */
	public static final char SEPARATOR = '@';

	/**
	 * Versions are dot-separated integers. {@code get_latest_version} upstream sorts
	 * them by {@code tuple(int(x) for x in v.split("_"))}, so anything non-numeric
	 * would crash there rather than here; rejecting it early gives a better message.
	 */
	private static final Pattern VERSION_PATTERN = Pattern.compile("\\d+(\\.\\d+)*");

	private final String name;
	private final String version;

	private BrainGlobeAtlasId(String name, String version) {
		this.name = name;
		this.version = version;
	}

	/**
	 * Builds an id from an already-split name and version.
	 *
	 * @param name    bare BrainGlobe atlas name, e.g. {@code allen_mouse_50um}
	 * @param version dotted version, e.g. {@code 3.1}
	 * @throws IllegalArgumentException if either part is malformed
	 */
	public static BrainGlobeAtlasId of(String name, String version) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Empty BrainGlobe atlas name");
		}
		if (name.indexOf(SEPARATOR) >= 0) {
			throw new IllegalArgumentException(
					"BrainGlobe atlas name must not contain '" + SEPARATOR + "': " + name);
		}
		if (version == null || !VERSION_PATTERN.matcher(version).matches()) {
			throw new IllegalArgumentException(
					"Invalid BrainGlobe atlas version '" + version + "' for atlas '" + name
							+ "': expected dot-separated integers, e.g. 3.1");
		}
		return new BrainGlobeAtlasId(name, version);
	}

	/**
	 * Parses {@code name@version}.
	 * <p>
	 * A bare name is rejected rather than resolved to the latest version. Silently
	 * picking a version is how an already-aligned dataset ends up reinterpreted
	 * against a different region numbering, so the ambiguity is refused outright.
	 *
	 * @param id the identifier to parse
	 * @throws IllegalArgumentException if {@code id} carries no version
	 */
	public static BrainGlobeAtlasId parse(String id) {
		if (id == null) {
			throw new IllegalArgumentException("Null BrainGlobe atlas id");
		}
		String trimmed = id.trim();
		int sep = trimmed.lastIndexOf(SEPARATOR); // last: names may not contain '@', but be explicit
		if (sep < 0) {
			throw new IllegalArgumentException(
					"BrainGlobe atlas '" + trimmed + "' has no version. Atlas versions change region "
							+ "ids, so one must be given explicitly, as in '" + trimmed + SEPARATOR + "3.1'.");
		}
		return of(trimmed.substring(0, sep), trimmed.substring(sep + 1));
	}

	/** @return true if {@code id} carries a {@code @} separator */
	public static boolean hasVersion(String id) {
		return id != null && id.indexOf(SEPARATOR) >= 0;
	}

	/** Formats {@code name@version} without building an instance */
	public static String format(String name, String version) {
		return name + SEPARATOR + version;
	}

	/** @return the bare BrainGlobe atlas name, e.g. {@code allen_mouse_50um} */
	public String getName() {
		return name;
	}

	/** @return the dotted version, e.g. {@code 3.1} — the form Python expects */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the version as it is spelled on disk, e.g. {@code 3_1}. BrainGlobe
	 *         stores each version in its own folder, underscored.
	 */
	public String getVersionFolder() {
		return version.replace('.', '_');
	}

	@Override
	public String toString() {
		return format(name, version);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof BrainGlobeAtlasId)) return false;
		BrainGlobeAtlasId other = (BrainGlobeAtlasId) o;
		return name.equals(other.name) && version.equals(other.version);
	}

	@Override
	public int hashCode() {
		return 31 * name.hashCode() + version.hashCode();
	}
}
