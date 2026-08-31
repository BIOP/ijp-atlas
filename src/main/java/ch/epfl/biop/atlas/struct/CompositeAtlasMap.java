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
			AtlasHelper.KEY_X, AtlasHelper.KEY_Y, AtlasHelper.KEY_Z, AtlasHelper.KEY_LEFT_RIGHT
	));

	/** All derived keys (dropped or prefixed from additional maps, kept only from principal at the end). */
	private static final Set<String> DERIVED_KEYS = new HashSet<>(Arrays.asList(
			AtlasHelper.KEY_BORDERS, AtlasHelper.KEY_X, AtlasHelper.KEY_Y, AtlasHelper.KEY_Z, AtlasHelper.KEY_LEFT_RIGHT
	));

	private final AtlasMap principalMap;
	private final Map<String, SourceAndConverter<?>> mergedImages = new LinkedHashMap<>();
	private final List<String> mergedKeys = new ArrayList<>();
	private final Map<String, AtlasMap> keyToSourceMap = new LinkedHashMap<>();
	private final Map<String, String> keyToOriginalKey = new LinkedHashMap<>();

	public CompositeAtlasMap(Atlas principalAtlas, List<Atlas> additionalAtlases) {
		this.principalMap = principalAtlas.getMap();
		String principalName = principalAtlas.getName();

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

		// 1. Add principal structural sources, prefixed with atlas name
		for (String key : principalStructuralKeys) {
			SourceAndConverter<?> source = principalMap.getStructuralImages().get(key);
			if (source != null) {
				String insertKey = principalName + " - " + key;
				mergedImages.put(insertKey, source);
				mergedKeys.add(insertKey);
				keyToSourceMap.put(insertKey, principalMap);
				keyToOriginalKey.put(insertKey, key);
			}
		}

		// 2. Add structural sources from additional atlases, prefixed with atlas name
		for (Atlas additionalAtlas : additionalAtlases) {
			AtlasMap additionalMap = additionalAtlas.getMap();
			String atlasName = additionalAtlas.getName();
			for (String key : additionalMap.getImagesKeys()) {
				// Always drop coordinate and left/right sources
				if (DROP_KEYS.contains(key)) continue;

				String insertKey = atlasName + " - " + key;
				if (mergedImages.containsKey(insertKey)) {
					logger.warning("CompositeAtlasMap: key '" + insertKey + "' from atlas '"
							+ atlasName + "' already exists, skipping");
					continue;
				}

				SourceAndConverter<?> source = additionalMap.getStructuralImages().get(key);
				if (source != null) {
					mergedImages.put(insertKey, source);
					mergedKeys.add(insertKey);
					keyToSourceMap.put(insertKey, additionalMap);
					keyToOriginalKey.put(insertKey, key);
				}
			}
		}

		// 3. Append principal derived sources at the end (no prefix)
		for (String key : principalDerivedKeys) {
			SourceAndConverter<?> source = principalMap.getStructuralImages().get(key);
			if (source != null) {
				mergedImages.put(key, source);
				mergedKeys.add(key);
				keyToSourceMap.put(key, principalMap);
				keyToOriginalKey.put(key, key);
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
			String originalKey = keyToOriginalKey.getOrDefault(key, key);
			return owner.getImageMax(originalKey);
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
