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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link AtlasMap} that combines a principal map with additional maps.
 * <p>
 * The principal map defines the label image, ontology-related sources,
 * coordinates, and all structural conventions (precision, coronal transform,
 * left/right labels). Additional maps contribute only their structural
 * image channels. If a key from an additional map collides with an existing
 * key, that source is silently dropped.
 */
public class CompositeAtlasMap implements AtlasMap {

	private final AtlasMap principalMap;
	private final Map<String, SourceAndConverter<?>> mergedImages = new LinkedHashMap<>();
	private final List<String> mergedKeys = new ArrayList<>();
	private final Map<String, AtlasMap> keyToSourceMap = new LinkedHashMap<>();

	public CompositeAtlasMap(AtlasMap principalMap, List<AtlasMap> additionalMaps) {
		this.principalMap = principalMap;

		// Add all principal sources first
		for (String key : principalMap.getImagesKeys()) {
			SourceAndConverter<?> sac = principalMap.getStructuralImages().get(key);
			if (sac != null) {
				mergedImages.put(key, sac);
				mergedKeys.add(key);
				keyToSourceMap.put(key, principalMap);
			}
		}

		// Add sources from additional maps, skipping collisions
		for (AtlasMap additionalMap : additionalMaps) {
			for (String key : additionalMap.getImagesKeys()) {
				if (!mergedImages.containsKey(key)) {
					SourceAndConverter<?> sac = additionalMap.getStructuralImages().get(key);
					if (sac != null) {
						mergedImages.put(key, sac);
						mergedKeys.add(key);
						keyToSourceMap.put(key, additionalMap);
					}
				}
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