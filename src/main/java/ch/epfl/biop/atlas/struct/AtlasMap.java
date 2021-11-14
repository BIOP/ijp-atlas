/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 EPFL
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
import java.util.List;
import java.util.Map;

/**
 * Interface to define an AtlasMap
 *
 * Pairs with AtlasOntology
 *
 * The Atlas Map contains :
 * - 3D images among which there are:
 *     - structural images, which are different modalities acquired for an atlas (fluorescence, brightfield)
 *     - a single Label Image
 *
 *
 */
public interface AtlasMap {

	/**
	 * Set where to catch the source of the Atlas, remote or local
	 * @param dataSource
	 */
	void setDataSource(URL dataSource);

	/**
	 * Triggers the initialisation of the Atlas
	 * @param atlasName
	 */
	void initialize(String atlasName);

	/**
	 * For convenience
	 * @return
	 */
	URL getDataSource();

	Map<String,SourceAndConverter> getStructuralImages();

	List<String> getImagesKeys();

	SourceAndConverter getLabelImage();

	Double getAtlasPrecisionInMillimeter();

	AffineTransform3D getCoronalTransform();

    Double getImageMax(String key);

    int labelRight();

	int labelLeft();
}
