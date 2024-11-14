/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2024 EPFL
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
package ch.epfl.biop.atlas.custom;

import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import ch.epfl.biop.bdv.img.imageplus.ImagePlusToSpimData;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import mpicbg.spim.data.generic.AbstractSpimData;
import net.imglib2.realtransform.AffineTransform3D;
import sc.fiji.bdvpg.services.SourceAndConverterServices;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
            public void initialize(URL mapURL, URL ontologyURL) throws Exception {

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
            Map<Integer, AtlasNode> idToNode = new HashMap<>();

            @Override
            public String getName() {
                return "Ontology-"+atlasName;
            }

            @Override
            public void initialize() throws Exception {
                HashSet<Integer> values = getUniquePixelValues(labelImage);
                values.forEach(id -> {
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
                    });
                });
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
                        return -1;
                    }

                    @Override
                    public int[] getColor() {
                        return new int[]{255,0,0,128};
                    }

                    @Override
                    public Map<String, String> data() {
                        Map<String,String> data = new HashMap<>();
                        data.put("name", "root");
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
                if (id == -1) {
                    return getRoot();
                } else {
                    return idToNode.get(id);
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

        SourceAndConverterServices.getSourceAndConverterService()
                .register(sd);

        List<SourceAndConverter<?>> structuralImages = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(sd);

        AbstractSpimData<?> sdLabel = ImagePlusToSpimData.getSpimData(label);

        SourceAndConverterServices.getSourceAndConverterService()
                .register(sdLabel);

        List<SourceAndConverter<?>> labelSource = SourceAndConverterServices
                .getSourceAndConverterService()
                .getSourceAndConverterFromSpimdata(sdLabel);

        AtlasMap map = fromSources(structuralImages.toArray(new SourceAndConverter[0]),
                    labelSource.get(0),atlasPrecisionMm
                );

        return makeAtlas(map, ontology, atlasName);
    }

    public static class AtlasMapFromSources implements AtlasMap {

        final Map<String, SourceAndConverter<?>> keyToImage = new HashMap<>();
        final List<String> imageKeys = new ArrayList<>();
        final SourceAndConverter<?> labelImage;

        final double atlasPixelSizeInMillimeter;

        public AtlasMapFromSources(SourceAndConverter<?>[] sources,
                                   SourceAndConverter<?> label,
                                   double atlasPixelSizeInMillimeter) {
            for (SourceAndConverter<?> source:sources) {
                imageKeys.add(source.getSpimSource().getName());
                keyToImage.put(source.getSpimSource().getName(), source);
            }
            keyToImage.put("X", AtlasHelper.getCoordinateSac(0, "X"));
            keyToImage.put("Y", AtlasHelper.getCoordinateSac(1, "Y"));
            keyToImage.put("Z", AtlasHelper.getCoordinateSac(2, "Z"));
            keyToImage.put("Left Right", label);

            imageKeys.add("X");
            imageKeys.add("Y");
            imageKeys.add("Z");
            imageKeys.add("Left Right");

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
            return 65535.0;
        }

        @Override
        public int labelRight() {
            return -1;
        }

        @Override
        public int labelLeft() {
            return -1;
        }
    }



}
