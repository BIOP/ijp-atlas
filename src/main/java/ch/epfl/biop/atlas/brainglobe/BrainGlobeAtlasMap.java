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
package ch.epfl.biop.atlas.brainglobe;

import bdv.cache.SharedQueue;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.util.VolatileSource;
import bdv.util.volatiles.VolatileTypeMatcher;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import static ch.epfl.biop.atlas.struct.AtlasHelper.*;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.source.SourceVoxelProcessor;
import io.scif.config.SCIFIOConfig;
import io.scif.img.ImgOpener;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.display.ColorConverter;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.scijava.Context;
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

	/**
	 * Default colour given to each structural channel, in display order: the
	 * reference first, then the additional references.
	 * <p>
	 * BDV composites channels additively over black, so each colour has to carry
	 * enough luminance on its own — which rules out the darker entries of the usual
	 * print-oriented colour-blind palettes. No channel is left white: an atlas is
	 * displayed over the section being aligned to it, and a white channel saturates
	 * every other one out of the picture. Amber and blue come first because that
	 * pair stays separable under protanopia, deuteranopia and tritanopia alike;
	 * magenta and green extend it as far as is honestly distinguishable. Beyond
	 * that, colour stops carrying information and every remaining channel is grey.
	 */
	private static final int[] CHANNEL_COLORS = {
			0xFFFFB000, // amber
			0xFF4DA6FF, // blue
			0xFFDC267F, // magenta
			0xFF00C08B, // green
	};

	/** Colour used once {@link #CHANNEL_COLORS} is exhausted */
	private static final int EXTRA_CHANNEL_COLOR = 0xFFB0B0B0;

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
	public void initializeFromApposeData(BrainGlobeAppose.BrainGlobeAtlasData data, Context ctx) {
		// Versioned, so that two versions of one atlas opened at once stay tellable
		// apart in the BDV source list
		this.atlasName = data.getId().toString();

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
		SourceAndConverter<?> referenceSource = loadTiffAsSourceAndConverter(data.referencePath, affine, atlasName + "_reference", ctx);
		structuralImages.put("reference", referenceSource);
		imageKeys.add("reference");
		maxValues.put("reference", 2*getMaxMiddlePlane(referenceSource));
		setChannelColor(referenceSource, 0);

		// Additional reference channels, in the order declared by the atlas manifest
		int channel = 1;
		for (Map.Entry<String, String> entry : data.additionalReferencePaths.entrySet()) {
			SourceAndConverter<?> source = loadTiffAsSourceAndConverter(entry.getValue(), affine, atlasName + "_" + entry.getKey(), ctx);
			structuralImages.put(entry.getKey(), source);
			imageKeys.add(entry.getKey());
			maxValues.put(entry.getKey(), 2*getMaxMiddlePlane(source));
			setChannelColor(source, channel++);
		}

		// Annotation/label image
		labelSource = loadTiffAsSourceAndConverter(data.annotationPath, affine, atlasName + "_annotation", ctx);

		// Borders derived from label image, plus coordinate sources (X, Y, Z)
		SourceAndConverter<?> bordersSource = SourceVoxelProcessor.getBorders(labelSource);
		maxValues.put(KEY_BORDERS, 256.0);
		AtlasHelper.addDerivedSources(structuralImages, imageKeys, bordersSource);

		// Left/Right indicator from hemispheres
		SourceAndConverter<?> leftRightSource;
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
			leftRightSource = SourceHelper.createSourceAndConverter(hemispheresSource);
		} else {
			// Non-symmetric atlas: load hemispheres.tiff from disk
			leftRightSource = loadTiffAsSourceAndConverter(data.hemispheresPath, affine, atlasName + "_hemispheres", ctx);
		}

		structuralImages.put(KEY_LEFT_RIGHT, leftRightSource);
		imageKeys.add(KEY_LEFT_RIGHT);
	}

	/**
	 * Gives a structural channel its default colour. The same converter instance
	 * backs the volatile and non-volatile sources, so a single call colours both.
	 *
	 * @param source  the channel to colour
	 * @param channel its index in display order, 0 being the reference
	 */
	private static void setChannelColor(SourceAndConverter<?> source, int channel) {
		if (!(source.getConverter() instanceof ColorConverter)) {
			// Not all pixel types yield a colourable converter; keep whatever it has
			return;
		}
		int color = channel < CHANNEL_COLORS.length
				? CHANNEL_COLORS[channel]
				: EXTRA_CHANNEL_COLOR;
		((ColorConverter) source.getConverter()).setColor(new ARGBType(color));
	}

	private<T extends RealType<T>> Double getMaxMiddlePlane(SourceAndConverter<?> source) {
		if (!(source.getSpimSource().getType() instanceof RealType)) {
			System.out.println("Can't auto adjust brightness of pixel type " + source.getSpimSource().getType().getClass().getSimpleName());
			return 65535.0;
		} else {
			RandomAccessibleInterval<T> img = (RandomAccessibleInterval<T>) source.getSpimSource().getSource(0, source.getSpimSource().getNumMipmapLevels() - 1);
			long zMiddle = (img.min(2) + img.max(2) + 1L) / 2L;
			Iterable<T> sampledPixels = Views.hyperSlice(img, 2, zMiddle);
			double minValue = Double.MAX_VALUE;
			double maxValue = -Double.MAX_VALUE;

			for(T pixel : sampledPixels) {
				double val = pixel.getRealDouble();
				if (val > maxValue) {
					maxValue = val;
				}
			}
			return maxValue;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static SourceAndConverter<?> loadTiffAsSourceAndConverter(String filePath, AffineTransform3D transform, String name, Context ctx) {
		SCIFIOConfig config = new SCIFIOConfig()
				.imgOpenerSetImgModes(SCIFIOConfig.ImgMode.CELL);

		ImgOpener opener = new ImgOpener(ctx);
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
