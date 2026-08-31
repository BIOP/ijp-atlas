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

import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.DiskCachedCellImgFactory;
import net.imglib2.cache.img.DiskCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.view.Views;

public class LabelImageHelper {

    public static <T extends Type<T> & Comparable<T> >  Img<ByteType>  get3DBorderLabelImage(RandomAccessibleInterval<T> lblImg) {
        // Make edge display on demand
        final int[] cellDimensions =  new int[]{32,32,32};

        // Cached Image Factory Options
        final DiskCachedCellImgOptions factoryOptions = DiskCachedCellImgOptions.options()
                .cellDimensions( cellDimensions )
                .cacheType( DiskCachedCellImgOptions.CacheType.BOUNDED )
                .maxCacheSize( 100 );

        // Expand label image by one pixel to avoid out of bounds exception
        final RandomAccessibleInterval<T> lblImgWithBorder =  Views.expandBorder(lblImg, 1,1,1);

        // Creates cached image factory of Type Byte
        final DiskCachedCellImgFactory<ByteType> factory = new DiskCachedCellImgFactory<>( new ByteType(), factoryOptions );

        // Creates shifted views by one pixel in each dimension
        RandomAccessibleInterval<T> lblImgXShift = Views.translate(lblImgWithBorder, 1,0,0);
        RandomAccessibleInterval<T> lblImgYShift = Views.translate(lblImgWithBorder, 0,1,0);
        RandomAccessibleInterval<T> lblImgZShift = Views.translate(lblImgWithBorder, 0,0,1);

        // Creates border image, with cell Consumer method, which creates the image
        final Img<ByteType> borderLabel = factory.create( lblImg, cell -> {

                // Cursor on the source image
        final Cursor<T> inNS = Views.flatIterable( Views.interval( lblImg, cell ) ).cursor();

        // Cursor on shifted source image
        final Cursor<T> inXS = Views.flatIterable( Views.interval( lblImgXShift, cell ) ).cursor();
        final Cursor<T> inYS = Views.flatIterable( Views.interval( lblImgYShift, cell ) ).cursor();
        final Cursor<T> inZS = Views.flatIterable( Views.interval( lblImgZShift, cell ) ).cursor();

        // Cursor on output image
        final Cursor<ByteType> out = Views.flatIterable( cell ).cursor();

        // Loops through voxels
        while ( out.hasNext() ) {
            T v = inNS.next();
            if (v.compareTo(inXS.next())!=0) {
                out.next().set( (byte) 126 );
                inYS.next();
                inZS.next();
            } else {
                if (v.compareTo(inYS.next())!=0) {
                    out.next().set( (byte) 126 );
                    inZS.next();
                } else {
                    if (v.compareTo(inZS.next())!=0) {
                        out.next().set( (byte) 126 );
                    } else {
                        out.next();
                    }
                }
            }
        }
        }, DiskCachedCellImgOptions.options().initializeCellsAsDirty( true ) );

        return borderLabel;
    }
}
