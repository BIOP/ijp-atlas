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
package ch.epfl.biop.atlas.struct;

import bdv.util.RealRandomAccessibleIntervalSource;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import ch.epfl.biop.atlas.mouse.allen.ccfv3.AllenOntology;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.imglib2.FinalInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.position.FunctionRealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.util.TreeNode;
import sc.fiji.bdvpg.sourceandconverter.SourceAndConverterHelper;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Static atlas ontology helper functions
 */
public class AtlasHelper {

    public static List<Integer> getAllParentIds(AtlasOntology ontology, int label) {
        AtlasNode origin = ontology.getNodeFromId(label);
        ArrayList listOfParentLabels = new ArrayList();
        if (origin == null) {
            return listOfParentLabels;
        }
        AtlasNode p = (AtlasNode) origin.parent();
        while (p!=null) {
            listOfParentLabels.add(p.getId());
            p = (AtlasNode) p.parent();
        }
        return listOfParentLabels;
    }

    public static Map<Integer, AtlasNode> buildIdToAtlasNodeMap(AtlasNode root) {
        Map<Integer, AtlasNode> result = new HashMap<>();
        return appendToIdToAtlasNodeMap(result, root);
    }

    private static Map<Integer, AtlasNode> appendToIdToAtlasNodeMap(Map<Integer, AtlasNode> map, AtlasNode node) {
        map.put(node.getId(), node);
        node.children().forEach(child -> appendToIdToAtlasNodeMap(map, (AtlasNode) child));
        return map;
    }

    public static SourceAndConverter<FloatType> getCoordinateSac(final int axis, String name) {
        BiConsumer<RealLocalizable, FloatType > coordIndicator = (l, t ) -> {
            t.set(l.getFloatPosition(axis));
        };

        FunctionRealRandomAccessible<FloatType> coordSource = new FunctionRealRandomAccessible(3,
                coordIndicator,	FloatType::new);

        final Source<FloatType> s = new RealRandomAccessibleIntervalSource<>( coordSource,
                FinalInterval.createMinMax( 0, 0, 0, 1320, 800, 1140),
                new FloatType(), new AffineTransform3D(), name );

        return SourceAndConverterHelper.createSourceAndConverter(s);
    }

    public static SourceAndConverter<FloatType> getCoordinateSacInvOffset(final int axis, final float offset, String name) {
        BiConsumer<RealLocalizable, FloatType > coordIndicator = (l, t ) -> {
            t.set(offset-l.getFloatPosition(axis));
        };

        FunctionRealRandomAccessible<FloatType> coordSource = new FunctionRealRandomAccessible(3,
                coordIndicator,	FloatType::new);

        final Source<FloatType> s = new RealRandomAccessibleIntervalSource<>( coordSource,
                FinalInterval.createMinMax( 0, 0, 0, 1320, 800, 1140),
                new FloatType(), new AffineTransform3D(), name );

        return SourceAndConverterHelper.createSourceAndConverter(s);
    }

    public static SourceAndConverter<FloatType> getCoordinateSacOffset(final int axis, final float offset, String name) {
        BiConsumer<RealLocalizable, FloatType > coordIndicator = (l, t ) -> {
            t.set(offset+l.getFloatPosition(axis));
        };

        FunctionRealRandomAccessible<FloatType> coordSource = new FunctionRealRandomAccessible(3,
                coordIndicator,	FloatType::new);

        final Source<FloatType> s = new RealRandomAccessibleIntervalSource<>( coordSource,
                FinalInterval.createMinMax( 0, 0, 0, 1320, 800, 1140),
                new FloatType(), new AffineTransform3D(), name );

        return SourceAndConverterHelper.createSourceAndConverter(s);
    }

    public static boolean saveOntologyToJsonFile(AtlasOntology ontology, String path) {
        try {

            FileWriter fw = new FileWriter(path);

            new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(new SerializableOntology(ontology), fw);

            fw.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static AtlasOntology openOntologyFromJsonFile(String path) {
        File ontologyFile = new File(path);
        if (ontologyFile.exists()) {
            Gson gson = new Gson();
            try {
                FileReader fr = new FileReader(ontologyFile.getAbsoluteFile());
                SerializableOntology ontology = gson.fromJson(new FileReader(ontologyFile.getAbsoluteFile()), SerializableOntology.class);
                ontology.initialize();
                fr.close();
                return ontology;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } else return null;
    }

    public static class SerializableOntology implements AtlasOntology{
        String name;
        String namingProperty;
        SerializableAtlasNode root;
        transient Map<Integer, AtlasNode> idToAtlasNodeMap;

        public SerializableOntology(AtlasOntology ontology) {
            this.name = ontology.getName();
            this.root = new SerializableAtlasNode(ontology.getRoot(), null);
            this.namingProperty = ontology.getNamingProperty();
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void initialize() throws Exception {
            idToAtlasNodeMap = AtlasHelper.buildIdToAtlasNodeMap(root);
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
            return root;
        }

        @Override
        public AtlasNode getNodeFromId(int id) {
            return idToAtlasNodeMap.get(id);
        }

        @Override
        public String getNamingProperty() {
            return namingProperty;
        }

        @Override
        public void setNamingProperty(String namingProperty) {
            this.namingProperty = namingProperty;
        }
    }

    public static class SerializableAtlasNode implements AtlasNode {

        final public int id;
        final public int[] color;
        final public Map<String, String> data;
        final public List<SerializableAtlasNode> children;

        transient final public SerializableAtlasNode parent;

        public SerializableAtlasNode(AtlasNode node, SerializableAtlasNode parent) {
            this.id = node.getId();
            this.data = node.data();
            this.parent = parent;
            this.color = node.getColor();
            children = new ArrayList<>();
            node.children().forEach(n -> {
                children.add(new SerializableAtlasNode(n, this));
            });
        }

        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public int[] getColor() {
            return color;
        }

        @Override
        public Map<String, String> data() {
            return data;
        }

        @Override
        public AtlasNode parent() {
            return parent;
        }

        @Override
        public List<? extends AtlasNode> children() {
            return children;
        }
    }

}
