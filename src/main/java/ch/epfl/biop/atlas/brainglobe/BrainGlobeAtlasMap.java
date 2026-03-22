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

import bdv.util.RandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.source.SourceVoxelProcessor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.appose.ShmImg;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import org.apposed.appose.NDArray;
import sc.fiji.bdvpg.source.SourceHelper;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AtlasMap implementation backed by BrainGlobe atlas data received via Appose.
 * <p>
 * Converts Appose NDArrays (shared memory) into BigDataViewer SourceAndConverter
 * objects with proper affine transforms based on the atlas resolution.
 * <p>
 * BrainGlobe atlases use ASR orientation (Anterior-Superior-Right), where:
 * - axis 0 = Anterior-Posterior
 * - axis 1 = Superior-Inferior
 * - axis 2 = Left-Right
 */
public class BrainGlobeAtlasMap implements AtlasMap {

	private final Map<String, SourceAndConverter<?>> structuralImages = new HashMap<>();
	private final List<String> imageKeys = new ArrayList<>();
	private final Map<String, Double> maxValues = new HashMap<>();

	private SourceAndConverter labelSource;
	private double precisionMm;
	private String atlasName;
	private URL dataSource;

	/**
	 * Initialize from BrainGlobe atlas data obtained via Appose.
	 *
	 * @param data the raw atlas data from BrainGlobeAppose.fetchAtlas()
	 */
	public void initializeFromApposeData(BrainGlobeAppose.BrainGlobeAtlasData data) {
		this.atlasName = data.getAtlasName();

		List<Double> resolution = data.getResolution(); // [z, y, x] in micrometers

		// Resolution in mm: BrainGlobe uses [z, y, x] order
		double voxXMm = resolution.get(2) / 1000.0;
		double voxYMm = resolution.get(1) / 1000.0;
		double voxZMm = resolution.get(0) / 1000.0;
		this.precisionMm = Math.min(voxXMm, Math.min(voxYMm, voxZMm));

		// Build affine transform: voxel to physical (mm)
		AffineTransform3D affine = new AffineTransform3D();
		affine.scale(voxXMm, voxYMm, voxZMm);

		// Reference image
		SourceAndConverter<?> referenceSac = ndArrayToSourceAndConverter(
				data.reference, affine, atlasName + "_reference");
		structuralImages.put("reference", referenceSac);
		imageKeys.add("reference");
		maxValues.put("reference", getMaxFromNDArray(data.reference));

		// Additional reference channels
		for (String refName : data.getAdditionalReferenceNames()) {
			NDArray arr = data.additionalReferences.get(refName);
			if (arr != null) {
				SourceAndConverter<?> sac = ndArrayToSourceAndConverter(
						arr, affine, atlasName + "_" + refName);
				structuralImages.put(refName, sac);
				imageKeys.add(refName);
				maxValues.put(refName, getMaxFromNDArray(arr));
			}
		}

		// Annotation/label image
		labelSource = ndArrayToSourceAndConverter(
				data.annotation, affine, atlasName + "_annotation");

		// Borders derived from label image
		SourceAndConverter<?> bordersSac = SourceVoxelProcessor.getBorders(labelSource);
		structuralImages.put("borders", bordersSac);
		imageKeys.add("borders");
		maxValues.put("borders", 256.0);

		// Coordinate sources (X, Y, Z)
		structuralImages.put("X", AtlasHelper.getCoordinateSac(0, "X"));
		structuralImages.put("Y", AtlasHelper.getCoordinateSac(1, "Y"));
		structuralImages.put("Z", AtlasHelper.getCoordinateSac(2, "Z"));
		imageKeys.add("X");
		imageKeys.add("Y");
		imageKeys.add("Z");

		// Left/Right indicator from hemispheres image
		SourceAndConverter<?> leftRightSac = ndArrayToSourceAndConverter(
				data.hemispheres, affine, atlasName + "_hemispheres");
		structuralImages.put("Left Right", leftRightSac);
		imageKeys.add("Left Right");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static SourceAndConverter<?> ndArrayToSourceAndConverter(
			NDArray ndArray, AffineTransform3D transform, String name) {

		// ShmImg wraps an NDArray as an ImgLib2 Img backed by shared memory
		Img img = new ShmImg<>(ndArray);

		Source source = new RandomAccessibleIntervalSource(
				img, (NumericType) img.getType(), transform, name);

		return SourceHelper.createSourceAndConverter(source);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static double getMaxFromNDArray(NDArray ndArray) {
		Img img = new ShmImg<>(ndArray);
		// Stream through and find max value
		return ((RandomAccessibleInterval<RealType<?>>) img)
				.parallelStream()
				.mapToDouble(ComplexType::getRealDouble)
				.max()
				.orElse(65535.0);
	}

	// --- AtlasMap interface ---

	@Override
	public void setDataSource(URL dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void initialize(String atlasName) {
		this.atlasName = atlasName;
	}

	@Override
	public URL getDataSource() {
		return dataSource;
	}

	@Override
	public Map<String, SourceAndConverter<?>> getStructuralImages() {
		return structuralImages;
	}

	@Override
	public List<String> getImagesKeys() {
		return imageKeys;
	}

	@Override
	public SourceAndConverter<?> getLabelImage() {
		return labelSource;
	}

	@Override
	public Double getAtlasPrecisionInMillimeter() {
		return precisionMm;
	}

	@Override
	public AffineTransform3D getCoronalTransform() {
		// BrainGlobe ASR: coronal view is the default (no transform needed)
		return new AffineTransform3D();
	}

	@Override
	public Double getImageMax(String key) {
		return maxValues.getOrDefault(key, 65535.0);
	}

	@Override
	public int labelRight() {
		// BrainGlobe convention: hemispheres image has 2 for right
		return 2;
	}

	@Override
	public int labelLeft() {
		// BrainGlobe convention: hemispheres image has 1 for left
		return 1;
	}

	@Override
	public String toString() {
		return atlasName;
	}
}
