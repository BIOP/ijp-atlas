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
package ch.epfl.biop.atlas.custom;

import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import static ch.epfl.biop.atlas.struct.AtlasHelper.*;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ch.epfl.biop.source.SourceVoxelProcessor;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.FinalInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import sc.fiji.bdvpg.service.SourceServices;
import sc.fiji.bdvpg.source.SourceHelper;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class AtlasFromSourcesHelper {

    public static AtlasMap fromSources(SourceAndConverter<?>[] sources, SourceAndConverter<?> label, double atlasPixelSizeMm) {
        return new AtlasMapFromSources(sources, label, atlasPixelSizeMm);
    }

    public static Atlas makeAtlas(AtlasMap map, AtlasOntology ontology, String name) {
        return new Atlas() {
            @Override
            public AtlasMap getMap() {
                return map;
            }

            @Override
            public AtlasOntology getOntology() {
                return ontology;
            }

            @Override
            public void initialize(URL mapURL, URL ontologyURL) {

            }

            @Override
            public List<String> getDOIs() {
                List<String> dois = new ArrayList<>();
                dois.add("no doi");
                return dois;
            }

            @Override
            public String getURL() {
                return null;
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    public static AtlasOntology dummyOntology() {
        return new AtlasOntology() {
            @Override
            public String getName() {
                return "No Ontology";
            }

            @Override
            public void initialize() throws Exception {

            }

            @Override
            public void setDataSource(URL dataSource) {

            }

            @Override
            public URL getDataSource() {
                return null;
            }

            @Override
            public AtlasNode getRoot() {
                return new AtlasNode() {
                    @Override
                    public Integer getId() {
                        return 0;
                    }

                    @Override
                    public int[] getColor() {
                        return new int[]{255,0,0,128};
                    }

                    @Override
                    public Map<String, String> data() {
                        Map<String,String> data = new HashMap<>();
                        data.put("name", "root");
                        data.put("id", Integer.toString(getId()));
                        return data;
                    }

                    @Override
                    public AtlasNode parent() {
                        return null;
                    }

                    @Override
                    public List<? extends AtlasNode> children() {
                        return new ArrayList<>();
                    }

                    @Override
                    public String toString() {
                        return "root";
                    }
                };
            }

            @Override
            public AtlasNode getNodeFromId(int id) {
                if (id == 0) {
                    return getRoot();
                } else {
                    return null;
                }
            }

            @Override
            public String getNamingProperty() {
                return "name";
            }

            @Override
            public void setNamingProperty(String namingProperty) {

            }
        };
    }

    public static AtlasOntology ontologyFromLabelImage(String atlasName, ImagePlus labelImage) {
        return new AtlasOntology() {
            final Map<Integer, AtlasNode> idToNode = new HashMap<>();
            int rootId;

            @Override
            public String getName() {
                return "Ontology-"+atlasName;
            }

            @Override
            public void initialize() {
                HashSet<Integer> values = getUniquePixelValues(labelImage);
                values.remove(0); // Causes issues otherwise
                int rootIdTest = 1024;
                while (values.contains(rootIdTest)) {
                    rootIdTest*=2;
                }
                rootId = rootIdTest;

                values.forEach(id ->
                    idToNode.put(id, new AtlasNode() {
                        @Override
                        public Integer getId() {
                            return id;
                        }

                        @Override
                        public int[] getColor() {
                            return new int[]{0,255,128,128};
                        }

                        @Override
                        public Map<String, String> data() {
                            Map<String,String> data = new HashMap<>();
                            data.put("name", Integer.toString(id));
                            data.put("id", Integer.toString(id));
                            return data;
                        }

                        @Override
                        public AtlasNode parent() {
                            return getRoot();
                        }

                        @Override
                        public List<? extends AtlasNode> children() {
                            return Collections.emptyList();
                        }

                        @Override
                        public String toString() {
                            return Integer.toString(id);
                        }
                    })
                );
            }

            @Override
            public void setDataSource(URL dataSource) {

            }

            @Override
            public URL getDataSource() {
                return null;
            }

            @Override
            public AtlasNode getRoot() {
                return new AtlasNode() {
                    @Override
                    public Integer getId() {
                        return rootId;
                    }

                    @Override
                    public int[] getColor() {
                        return new int[]{128,0,0,128};
                    }

                    @Override
                    public Map<String, String> data() {
                        Map<String,String> data = new HashMap<>();
                        data.put("name", "root");
                        data.put("id", Integer.toString(getId()));
                        return data;
                    }

                    @Override
                    public AtlasNode parent() {
                        return null;
                    }

                    @Override
                    public List<? extends AtlasNode> children() {
                        return new ArrayList<>(idToNode.values());
                    }

                    @Override
                    public String toString() {
                        return "root";
                    }
                };
            }

            @Override
            public AtlasNode getNodeFromId(int id) {
                if (id == rootId) {
                    return getRoot();
                } else {
                    return idToNode.get(id);
                }
            }

            @Override
            public String getNamingProperty() {
                return "id";
            }

            @Override
            public void setNamingProperty(String namingProperty) {

            }
        };
    }

    public static HashSet<Integer> getUniquePixelValues(ImagePlus image) {
        HashSet<Integer> uniqueValues = new HashSet<>();

        int stackSize = image.getStackSize();

        // Iterate through each slice in the stack
        for (int i = 1; i <= stackSize; i++) {
            ImageProcessor ip = image.getStack().getProcessor(i);

            // Iterate through each pixel in the slice
            for (int y = 0; y < ip.getHeight(); y++) {
                for (int x = 0; x < ip.getWidth(); x++) {
                    int pixelValue = ip.get(x, y);
                    uniqueValues.add(pixelValue);
                }
            }
        }

        return uniqueValues;
    }

    public static Atlas fromImagePlus(String atlasName, ImagePlus image, ImagePlus label, double atlasPrecisionMm) {
        AtlasOntology ontology;
        if (label == null) {
            ontology = dummyOntology();
        } else {
            ontology = ontologyFromLabelImage(atlasName, label);
        }

        AbstractSpimData<?> sd = ImagePlusToSpimData.getSpimData(image);

        SourceServices.getSourceService()
                .register(sd);

        List<SourceAndConverter<?>> structuralImages = SourceServices
                .getSourceService()
                .getSourcesFromDataset(sd);

        AbstractSpimData<?> sdLabel = ImagePlusToSpimData.getSpimData(label);

        SourceServices.getSourceService()
                .register(sdLabel);

        List<SourceAndConverter<?>> labelSource = SourceServices
                .getSourceService()
                .getSourcesFromDataset(sdLabel);

        AtlasMap map = fromSources(structuralImages.toArray(new SourceAndConverter[0]),
                    labelSource.get(0),atlasPrecisionMm
                );

        return makeAtlas(map, ontology, atlasName);
    }

    public static class AtlasMapFromSources implements AtlasMap {

        final Map<String, SourceAndConverter<?>> keyToImage = new HashMap<>();
        final List<String> imageKeys = new ArrayList<>();
        final SourceAndConverter<?> labelImage;
        final Map<String, Double> maximaPerChannel = new HashMap<>();

        final double atlasPixelSizeInMillimeter;

        public AtlasMapFromSources(SourceAndConverter<?>[] sources,
                                   SourceAndConverter label,
                                   double atlasPixelSizeInMillimeter) {
            for (SourceAndConverter<?> source:sources) {
                imageKeys.add(source.getSpimSource().getName());
                keyToImage.put(source.getSpimSource().getName(), source);
                maximaPerChannel.put(source.getSpimSource().getName(), getMax((Source<RealType<?>>) source.getSpimSource()));
            }
            SourceAndConverter<?> borderSource = label != null ? SourceVoxelProcessor.getBorders(label) : null;
            if (label!=null) maximaPerChannel.put(KEY_BORDERS, 256.0);

            AtlasHelper.addDerivedSources(keyToImage, imageKeys, borderSource);

            AffineTransform3D at3D = new AffineTransform3D();
            sources[0].getSpimSource().getSourceTransform(0,0, at3D);

            final double thresholdX = (double) sources[0].getSpimSource().getSource(0, 0).dimension(0) /2 * at3D.get(0,0);

            BiConsumer<RealLocalizable, UnsignedShortType > leftRightIndicator = (l, t ) -> {
                if (l.getFloatPosition(0)>thresholdX) {
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

            keyToImage.put(KEY_LEFT_RIGHT, leftRight);
            imageKeys.add(KEY_LEFT_RIGHT);

            labelImage = label;
            this.atlasPixelSizeInMillimeter = atlasPixelSizeInMillimeter;
        }

        @Override
        public void setDataSource(URL dataSource) {

        }

        @Override
        public void initialize(String atlasName) {

        }

        @Override
        public URL getDataSource() {
            return null;
        }

        @Override
        public Map<String, SourceAndConverter<?>> getStructuralImages() {
            return keyToImage;
        }

        @Override
        public List<String> getImagesKeys() {
            return imageKeys;
        }

        @Override
        public SourceAndConverter<?> getLabelImage() {
            return labelImage;
        }

        @Override
        public Double getAtlasPrecisionInMillimeter() {
            return atlasPixelSizeInMillimeter;
        }

        @Override
        public AffineTransform3D getCoronalTransform() {
            return new AffineTransform3D();
        }

        @Override
        public Double getImageMax(String key) {
            return maximaPerChannel.getOrDefault(key, 65535.0);
        }

        @Override
        public int labelRight() {
            return 0;
        }

        @Override
        public int labelLeft() {
            return 255;
        }
    }

    private static double getMax(Source<RealType<?>> source) {
        return source.getSource(0,0).parallelStream().mapToDouble(ComplexType::getRealDouble).max().getAsDouble();
    }

}
