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
package ch.epfl.biop.atlas.struct;

import bdv.viewer.SourceAndConverter;
import net.imglib2.realtransform.AffineTransform3D;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * An {@link AtlasMap} that combines a principal map with additional maps.
 * <p>
 * The principal map defines the label image, ontology-related sources,
 * coordinates, and all structural conventions (precision, coronal transform,
 * left/right labels). Additional maps contribute only their structural
 * image channels — the coordinate and left/right sources (X, Y, Z, Left Right)
 * are stripped from additional maps. Other colliding keys (like borders or
 * structural images) are prefixed with the atlas name.
 */
public class CompositeAtlasMap implements AtlasMap {

	private static final Logger logger = Logger.getLogger(CompositeAtlasMap.class.getName());

	/** Keys that are always dropped from additional maps (no prefix, just skip). */
	private static final Set<String> DROP_KEYS = new HashSet<>(Arrays.asList(
			"X", "Y", "Z", "Left Right"
	));

	/** All derived keys (dropped or prefixed from additional maps, kept only from principal at the end). */
	private static final Set<String> DERIVED_KEYS = new HashSet<>(Arrays.asList(
			"borders", "Label Borders", "Labels Border", "X", "Y", "Z", "Left Right"
	));

	private final AtlasMap principalMap;
	private final Map<String, SourceAndConverter<?>> mergedImages = new LinkedHashMap<>();
	private final List<String> mergedKeys = new ArrayList<>();
	private final Map<String, AtlasMap> keyToSourceMap = new LinkedHashMap<>();

	public CompositeAtlasMap(Atlas principalAtlas, List<Atlas> additionalAtlases) {
		this.principalMap = principalAtlas.getMap();

		List<String> principalKeys = principalMap.getImagesKeys();

		// Split principal keys into structural and derived
		List<String> principalStructuralKeys = new ArrayList<>();
		List<String> principalDerivedKeys = new ArrayList<>();
		for (String key : principalKeys) {
			if (DERIVED_KEYS.contains(key)) {
				principalDerivedKeys.add(key);
			} else {
				principalStructuralKeys.add(key);
			}
		}

		// 1. Add principal structural sources
		for (String key : principalStructuralKeys) {
			SourceAndConverter<?> sac = principalMap.getStructuralImages().get(key);
			if (sac != null) {
				mergedImages.put(key, sac);
				mergedKeys.add(key);
				keyToSourceMap.put(key, principalMap);
			}
		}

		// 2. Add structural sources from additional atlases
		for (Atlas additionalAtlas : additionalAtlases) {
			AtlasMap additionalMap = additionalAtlas.getMap();
			String atlasName = additionalAtlas.getName();
			for (String key : additionalMap.getImagesKeys()) {
				// Always drop coordinate and left/right sources
				if (DROP_KEYS.contains(key)) continue;

				String insertKey = key;
				if (mergedImages.containsKey(key)) {
					// Collision: prefix with atlas name
					insertKey = atlasName + "_" + key;
					logger.warning("CompositeAtlasMap: key '" + key + "' from atlas '"
							+ atlasName + "' collides with existing key, renamed to '" + insertKey + "'");
					if (mergedImages.containsKey(insertKey)) {
						logger.warning("CompositeAtlasMap: prefixed key '" + insertKey
								+ "' still collides, skipping");
						continue;
					}
				}

				SourceAndConverter<?> sac = additionalMap.getStructuralImages().get(key);
				if (sac != null) {
					mergedImages.put(insertKey, sac);
					mergedKeys.add(insertKey);
					keyToSourceMap.put(insertKey, additionalMap);
				}
			}
		}

		// 3. Append principal derived sources at the end
		for (String key : principalDerivedKeys) {
			SourceAndConverter<?> sac = principalMap.getStructuralImages().get(key);
			if (sac != null) {
				mergedImages.put(key, sac);
				mergedKeys.add(key);
				keyToSourceMap.put(key, principalMap);
			}
		}
	}

	@Override
	public void setDataSource(URL dataSource) {
		principalMap.setDataSource(dataSource);
	}

	@Override
	public void initialize(String atlasName) {
		// Already initialized via constructor
	}

	@Override
	public URL getDataSource() {
		return principalMap.getDataSource();
	}

	@Override
	public Map<String, SourceAndConverter<?>> getStructuralImages() {
		return mergedImages;
	}

	@Override
	public List<String> getImagesKeys() {
		return mergedKeys;
	}

	@Override
	public SourceAndConverter<?> getLabelImage() {
		return principalMap.getLabelImage();
	}

	@Override
	public Double getAtlasPrecisionInMillimeter() {
		return principalMap.getAtlasPrecisionInMillimeter();
	}

	@Override
	public AffineTransform3D getCoronalTransform() {
		return principalMap.getCoronalTransform();
	}

	@Override
	public Double getImageMax(String key) {
		AtlasMap owner = keyToSourceMap.get(key);
		if (owner != null) {
			return owner.getImageMax(key);
		}
		return principalMap.getImageMax(key);
	}

	@Override
	public int labelRight() {
		return principalMap.labelRight();
	}

	@Override
	public int labelLeft() {
		return principalMap.labelLeft();
	}
}