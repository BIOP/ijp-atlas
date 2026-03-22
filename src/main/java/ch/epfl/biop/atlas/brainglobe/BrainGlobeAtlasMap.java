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

import bdv.cache.SharedQueue;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.util.VolatileSource;
import bdv.util.volatiles.VolatileTypeMatcher;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.source.SourceVoxelProcessor;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgOpener;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import sc.fiji.bdvpg.source.SourceHelper;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AtlasMap implementation backed by BrainGlobe atlas data received via Appose.
 * <p>
 * Image data is loaded from TIFF files on disk (stored by brainglobe-atlasapi)
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
	 * Image data is loaded from TIFF file paths on disk.
	 *
	 * @param data the atlas data from BrainGlobeAppose.fetchAtlas()
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
		SourceAndConverter<?> referenceSac = loadTiffAsSourceAndConverter(data.referencePath, affine, atlasName + "_reference");
		structuralImages.put("reference", referenceSac);
		imageKeys.add("reference");

		// Additional reference channels
		for (Map.Entry<String, String> entry : data.additionalReferencePaths.entrySet()) {
			SourceAndConverter<?> sac = loadTiffAsSourceAndConverter(entry.getValue(), affine, atlasName + "_" + entry.getKey());
			structuralImages.put(entry.getKey(), sac);
			imageKeys.add(entry.getKey());
		}

		// Annotation/label image
		labelSource = loadTiffAsSourceAndConverter(data.annotationPath, affine, atlasName + "_annotation");

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

		// Left/Right indicator from hemispheres
		SourceAndConverter<?> leftRightSac;
		if (data.isSymmetric()) {
			// Symmetric atlas: generate hemispheres procedurally by splitting along the frontal axis.
			// Fill with 2, then set the second half (from round(size/2) onward) to 1.
			// This matches brainglobe's Python logic.
			long[] shapeZYX = data.getShape();
			// BrainGlobe shape is [z, y, x], ImgLib2 RAI is [x, y, z] — reverse
			long[] shape = new long[]{shapeZYX[2], shapeZYX[1], shapeZYX[0]};
			// Frontal axis index from Python is in ZYX order — flip: 0->2, 1->1, 2->0
			int frontalAxis = 2-data.getFrontalAxisIndex();
			long splitAt = Math.round(shape[frontalAxis] / 2.0);
			FunctionRandomAccessible<UnsignedByteType> hemispheresFra = new FunctionRandomAccessible<>(3,
					(pos, val) -> val.set(pos.getLongPosition(frontalAxis) < splitAt ? 2 : 1),
					UnsignedByteType::new);
			RandomAccessibleInterval<UnsignedByteType> hemispheresRai = Views.interval(hemispheresFra,
					new FinalInterval(shape));
			Source<UnsignedByteType> hemispheresSource = new RandomAccessibleIntervalSource<>(
					hemispheresRai, new UnsignedByteType(), affine, atlasName + "_hemispheres");
			leftRightSac = SourceHelper.createSourceAndConverter(hemispheresSource);
		} else {
			// Non-symmetric atlas: load hemispheres.tiff from disk
			leftRightSac = loadTiffAsSourceAndConverter(data.hemispheresPath, affine, atlasName + "_hemispheres");
		}

		structuralImages.put("Left Right", leftRightSac);
		imageKeys.add("Left Right");
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static SourceAndConverter<?> loadTiffAsSourceAndConverter(String filePath, AffineTransform3D transform, String name) {
		SCIFIOConfig config = new SCIFIOConfig()
				.imgOpenerSetImgModes(SCIFIOConfig.ImgMode.CELL);

		ImgOpener opener = new ImgOpener();
		RandomAccessibleInterval rai = opener
				.openImgs(filePath, config)
				.get(0);

		Source src = new RandomAccessibleIntervalSource(
				rai, (NumericType) rai.getType(), transform, name);

		Converter converter = SourceHelper.createConverterRealType((RealType) src.getType());

		VolatileSource vSrc = new VolatileSource(src,
                (Volatile) VolatileTypeMatcher.getVolatileTypeForType((NativeType)src.getType()),
				new SharedQueue(10,1));

		SourceAndConverter vsource = new SourceAndConverter(vSrc, converter);

		return new SourceAndConverter<>(src, converter, vsource);
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
