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
        final RandomAccessibleInterval<T> lblImgWithBorder =  Views.expandBorder(lblImg,new long[]{1,1,1});

        // Creates cached image factory of Type Byte
        final DiskCachedCellImgFactory<ByteType> factory = new DiskCachedCellImgFactory<>( new ByteType(), factoryOptions );

        // Creates shifted views by one pixel in each dimension
        RandomAccessibleInterval<T> lblImgXShift = Views.translate(lblImgWithBorder,new long[]{1,0,0});
        RandomAccessibleInterval<T> lblImgYShift = Views.translate(lblImgWithBorder,new long[]{0,1,0});
        RandomAccessibleInterval<T> lblImgZShift = Views.translate(lblImgWithBorder,new long[]{0,0,1});

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
