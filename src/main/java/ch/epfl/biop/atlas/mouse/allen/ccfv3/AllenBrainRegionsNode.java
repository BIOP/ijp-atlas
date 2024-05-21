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
package ch.epfl.biop.atlas.mouse.allen.ccfv3;

import ch.epfl.biop.atlas.struct.AtlasNode;
import java.util.*;
import java.util.List;

public class AllenBrainRegionsNode implements AtlasNode {
    transient final AllenOntologyJson.AllenBrainRegion abr;
    transient final AllenBrainRegionsNode parent;
    final Map<String, String> properties;
    final List<AtlasNode> children;

    final int[] color;

    public static String toStringKey = "acronym";

    public AllenBrainRegionsNode(AllenOntologyJson.AllenBrainRegion abr, AllenBrainRegionsNode parent) {
        this.abr = abr;
        this.parent = parent;
        Map<String, String> mutableMap = new HashMap<>();
        mutableMap.put("id", Integer.toString(abr.id));
        mutableMap.put("atlas_id", Integer.toString(abr.atlas_id));
        mutableMap.put("ontology_id", Integer.toString(abr.ontology_id));
        mutableMap.put("acronym", abr.acronym);
        mutableMap.put("name", abr.name);
        mutableMap.put("color_hex_triplet", abr.color_hex_triplet);
        mutableMap.put("graph_order", Integer.toString(abr.graph_order));
        mutableMap.put("st_level", Integer.toString(abr.st_level));
        mutableMap.put("hemisphere_id", Integer.toString(abr.hemisphere_id));
        mutableMap.put("parent_structure_id", Integer.toString(abr.parent_structure_id));
        properties = Collections.unmodifiableMap(mutableMap);
        children = new ArrayList<>(abr.children.size());
        color = hex2Rgba(abr.color_hex_triplet);
        abr.children.forEach(child_abr -> {
            children.add(new AllenBrainRegionsNode(child_abr, this));
        });
    }

    @Override
    public Integer getId() {
        return abr.id;
    }
    /**
     * https://stackoverflow.com/questions/4129666/how-to-convert-hex-to-rgb-using-java
     * @param colorStr e.g. "#FFFFFF"
     * @return
     */
    public static int[] hex2Rgba(String colorStr) {
        return new int[]{
                Integer.valueOf( colorStr.substring( 0, 2 ), 16 ),
                Integer.valueOf( colorStr.substring( 2, 4 ), 16 ),
                Integer.valueOf( colorStr.substring( 4, 6 ), 16 ),255 };
    }

    @Override
    public int[] getColor() {
        return color;
    }

    @Override
    public Map<String, String> data() {
        return properties;
    }

    @Override
    public AllenBrainRegionsNode parent() {
        return parent;
    }

    @Override
    public List<AtlasNode> children() {
        return children;
    }

    @Override
    public String toString() {
        return data().get(toStringKey);
    }
}
