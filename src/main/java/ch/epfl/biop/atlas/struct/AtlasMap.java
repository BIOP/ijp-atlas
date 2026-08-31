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
import java.util.List;
import java.util.Map;

/**
 * Interface to define an AtlasMap
 * <br>
 * Pairs with AtlasOntology
 * <br>
 * The Atlas Map contains :
 * - 3D images among which there are:
 *     - structural images, which are different modalities acquired for an atlas (fluorescence, brightfield)
 *     - a single Label Image
 */
public interface AtlasMap {

	/**
	 * Set where to catch the source of the Atlas, remote or local
	 * @param dataSource
	 */
	void setDataSource(URL dataSource);

	/**
	 * Triggers the initialisation of the Atlas
	 * @param atlasName name to give to the atlas
	 */
	void initialize(String atlasName);

	/**
	 * For convenience
	 * @return the original URL of where the data was read from
	 */
	URL getDataSource();

	Map<String,SourceAndConverter<?>> getStructuralImages();

	List<String> getImagesKeys();

	SourceAndConverter<?> getLabelImage();

	Double getAtlasPrecisionInMillimeter();

	AffineTransform3D getCoronalTransform();

    Double getImageMax(String key);

    int labelRight();

	int labelLeft();
}
