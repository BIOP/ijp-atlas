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
