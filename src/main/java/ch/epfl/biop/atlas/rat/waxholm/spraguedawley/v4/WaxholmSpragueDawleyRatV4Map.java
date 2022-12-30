/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2022 EPFL
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

public class WaxholmSpragueDawleyRatV4Map implements AtlasMap {

    URL dataSource;

    public String name;

    final Map<String,SourceAndConverter> atlasSources = new HashMap<>();

    SourceAndConverter labelSource;

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

        SpimDataFromXmlImporter importer = new SpimDataFromXmlImporter(address);

        final List<SourceAndConverter<?>> sacs = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(importer.get());

        atlasSources.put("Structure", sacs.get(0));

        atlasSources.put("Labels Border", sacs.get(1));

        labelSource = sacs.get(2);

        BiConsumer<RealLocalizable, UnsignedShortType> leftRightIndicator = (l, t ) -> {
            if (l.getFloatPosition(0)<0) {
                t.set(255);
            } else {
                t.set(0);
            }
        };

        FunctionRealRandomAccessible leftRightSource = new FunctionRealRandomAccessible(3,
                leftRightIndicator,	UnsignedShortType::new);

        final Source< UnsignedShortType > s = new RealRandomAccessibleIntervalSource<>( leftRightSource,
                FinalInterval.createMinMax( 0, 0, 0, 1000, 1000, 0),
                new UnsignedShortType(), new AffineTransform3D(), "Left_Right" );

        SourceAndConverter leftRight = SourceAndConverterHelper.createSourceAndConverter(s);

        atlasSources.put("Left Right", leftRight);

        SourceAndConverterServices.getSourceAndConverterService().register(leftRight);

        SourceAndConverter<FloatType> xSource = getCoordinateSac(0,"X");
        SourceAndConverter<FloatType> ySource = getCoordinateSac(1,"Y");
        SourceAndConverter<FloatType> zSource = getCoordinateSac(2,"Z");

        atlasSources.put("X", xSource);
        atlasSources.put("Y", ySource);
        atlasSources.put("Z", zSource);

        SourceAndConverterServices.getSourceAndConverterService().register(xSource);
        SourceAndConverterServices.getSourceAndConverterService().register(ySource);
        SourceAndConverterServices.getSourceAndConverterService().register(zSource);
    }

    @Override
    public Map<String,SourceAndConverter> getStructuralImages() {
        return atlasSources;
    }

    @Override
    public List<String> getImagesKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("Structure");
        keys.add("Labels Border");
        keys.add("X");
        keys.add("Y");
        keys.add("Z");
        keys.add("Left Right");
        return keys;
    }

    @Override
    public SourceAndConverter getLabelImage() {
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
            case "Labels Border": return (double) 1024;
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
