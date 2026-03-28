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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4;

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
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.source.SourceHelper;
import sc.fiji.bdvpg.dataset.importer.XMLToDatasetImporter;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static ch.epfl.biop.atlas.struct.AtlasHelper.*;

public class WaxholmSpragueDawleyRatV4Map implements AtlasMap {

    URL dataSource;

    public String name;

    final Map<String,SourceAndConverter<?>> atlasSources = new HashMap<>();

    SourceAndConverter<?> labelSource;

    @Override
    public void setDataSource(URL dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public URL getDataSource() {
        return dataSource;
    }

    @Override
    public void initialize(String atlasName) {
        this.name = atlasName;

        String address =  this.getDataSource().toString();
        // Hacky Mac HackFace
        if (address.startsWith("file:")) {
            address = address.substring(5).replaceAll("%20", " ");
        }

        XMLToDatasetImporter importer = new XMLToDatasetImporter(address);

        final List<SourceAndConverter<?>> sources = SourceServices
                .getSourceService()
                .getSourcesFromDataset(importer.get());

        atlasSources.put("Structure", sources.get(0));

        atlasSources.put(KEY_BORDERS, sources.get(1));

        labelSource = sources.get(2);

        BiConsumer<RealLocalizable, UnsignedShortType> leftRightIndicator = (l, t ) -> {
            if (l.getFloatPosition(0)<0) {
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

        SourceAndConverter<?> leftRight = SourceHelper.createSourceAndConverter(s);

        atlasSources.put(KEY_LEFT_RIGHT, leftRight);

        SourceServices.getSourceService().register(leftRight);

        SourceAndConverter<FloatType> xSource = getCoordinateSource(0, KEY_X);
        SourceAndConverter<FloatType> ySource = getCoordinateSource(1, KEY_Y);
        SourceAndConverter<FloatType> zSource = getCoordinateSource(2, KEY_Z);

        atlasSources.put(KEY_X, xSource);
        atlasSources.put(KEY_Y, ySource);
        atlasSources.put(KEY_Z, zSource);

        SourceServices.getSourceService().register(xSource);
        SourceServices.getSourceService().register(ySource);
        SourceServices.getSourceService().register(zSource);
    }

    @Override
    public Map<String,SourceAndConverter<?>> getStructuralImages() {
        return atlasSources;
    }

    @Override
    public List<String> getImagesKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("Structure");
        keys.add(KEY_BORDERS);
        keys.add(KEY_X);
        keys.add(KEY_Y);
        keys.add(KEY_Z);
        keys.add(KEY_LEFT_RIGHT);
        return keys;
    }

    @Override
    public SourceAndConverter<?> getLabelImage() {
        return labelSource;
    }

    @Override
    public Double getAtlasPrecisionInMillimeter() {
        return 0.039;
    }

    @Override
    public AffineTransform3D getCoronalTransform() {
        AffineTransform3D at3d = new AffineTransform3D();
        at3d.rotate(0,-Math.PI/2);
        at3d.rotate(2,Math.PI);
        return at3d;
    }

    @Override
    public Double getImageMax(String key) {
        switch (key) {
            case "Structure": return (double) 80000;
            case KEY_BORDERS: return (double) 1024;
            default: return (double) 65535;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override // TODO : fix in the image construction
    public int labelRight() {
        return 0;
    }

    @Override
    public int labelLeft() {
        return 255;
    }

}
