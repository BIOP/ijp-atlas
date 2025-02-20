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
package ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr;

import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.struct.AtlasMap;
import net.imglib2.FinalInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import sc.fiji.bdvpg.services.SourceAndConverterServices;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;
import sc.fiji.bdvpg.spimdata.importer.SpimDataFromXmlImporter;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static ch.epfl.biop.atlas.struct.AtlasHelper.getCoordinateSac;

public class AllenMapASR implements AtlasMap {

	URL dataSource;

	public String name;

	// Original source order in xml / hdf5 file
	final static private int NisslSetupId = 0;
	final static private int LabelBorberSetupId = 1;
	final static private int AraSetupId = 2;
	final static private int LabelSetupId = 3;

	final Map<String,SourceAndConverter<?>> atlasSources = new HashMap<>();

	SourceAndConverter<?> labelSource;

	@Override
	public void initialize(String atlasName) {
		this.name = atlasName;

		String address =  this.getDataSource().toString();
		// Hacky Mac HackFace
		if (address.startsWith("file:")) {
			address = address.substring(5).replaceAll("%20", " ");
		}

		// Fix a stupid issue... For some reason the path may contain an extra slash in windows
		if (System.getProperty("os.name").startsWith("Windows") && address.startsWith("/")) {
			address = address.substring(1);
		}

		SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(address);

		final List<SourceAndConverter<?>> sacs = SourceAndConverterServices
			.getSourceAndConverterService()
			.getSourceAndConverterFromSpimdata(importer.get());

		atlasSources.put("Ara", sacs.get(AraSetupId));
		atlasSources.put("Nissl", sacs.get(NisslSetupId));
		atlasSources.put("Label Borders", sacs.get(AllenMapASR.LabelBorberSetupId));
		labelSource = sacs.get(AllenMapASR.LabelSetupId);

		BiConsumer<RealLocalizable, UnsignedShortType > leftRightIndicator = (l, t ) -> {
			if (l.getFloatPosition(0)>5.7) { // 11.4 mm / 2
				t.set(255);
			} else {
				t.set(0);
			}
		};

		FunctionRealRandomAccessible<UnsignedShortType> leftRightSource = new FunctionRealRandomAccessible<>(3,
				leftRightIndicator,	UnsignedShortType::new);

		final Source< UnsignedShortType > s = new RealRandomAccessibleIntervalSource<>( leftRightSource,
				FinalInterval.createMinMax( 0, 0, 0, 1000, 1000, 0),
				new UnsignedShortType(), new AffineTransform3D(), "Left_Right" );

		SourceAndConverter<?> leftRight = SourceAndConverterHelper.createSourceAndConverter(s);

		atlasSources.put("Left Right", leftRight);

		SourceAndConverterServices.getSourceAndConverterService().register(leftRight);

		SourceAndConverter<FloatType> xSource = getCoordinateSac(0,"X");
		SourceAndConverter<FloatType> ySource = getCoordinateSac(1,"Y");
		SourceAndConverter<FloatType> zSource = getCoordinateSac(2,"Z");

		atlasSources.put("X", xSource);
		atlasSources.put("Y", ySource);
		atlasSources.put("Z", zSource);

	}

	@Override
	public void setDataSource(URL dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public URL getDataSource() {
		return dataSource;
	}

	@Override
	public Map<String,SourceAndConverter<?>> getStructuralImages() {
		return atlasSources;
	}

	@Override
	public List<String> getImagesKeys() {
		List<String> keys = new ArrayList<>();
		keys.add("Nissl");
		keys.add("Ara");
		keys.add("Label Borders");
		keys.add("X");
		keys.add("Y");
		keys.add("Z");
		keys.add("Left Right");
		return keys;
	}

	@Override
	public SourceAndConverter<?> getLabelImage() {
		return labelSource;
	}

	@Override
	public Double getAtlasPrecisionInMillimeter() {
		return 0.010; // 10 micrometer
	}

	@Override
	public AffineTransform3D getCoronalTransform() {
		return new AffineTransform3D();
	}

	@Override
	public Double getImageMax(String key) {
		switch (key) {
			case "Nissl": return (double) 56000;
			case "Ara": return (double) 1024;
			case "Label Borders": return (double) 1024;
			default: return (double) 65535;
		}
	}

	@Override
	public int labelRight() {
		return 0;
	}

	@Override
	public int labelLeft() {
		return 255;
	}

	@Override
	public String toString() {
		return name;
	}
}
