/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2023 EPFL
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

import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.ilfparser.Label;
import ch.epfl.biop.atlas.struct.AtlasNode;

import java.awt.*;
import java.util.*;
import java.util.List;

public class WHSDRegionsNode implements AtlasNode {
    transient final Label label;
    transient final WHSDRegionsNode parent;
    final Map<String, String> properties;
    final List<AtlasNode> children;
    transient final Integer id;
    int[] color;

    public static String toStringKey = "abbreviation";

    public WHSDRegionsNode(Label label, WHSDRegionsNode parent) {
        this.label = label;
        this.parent = parent;
        Map<String, String> mutableMap = new HashMap<>();
        mutableMap.put("id", label.id);
        mutableMap.put("abbreviation", label.abbreviation);
        mutableMap.put("name", label.name);
        mutableMap.put("color", label.color);
        color = hex2Rgb(label.color);
        properties = Collections.unmodifiableMap(mutableMap);
        if (label.labels!=null) {
            children = new ArrayList<>(label.labels.length);
            for (Label childLabel : label.labels) {
                children.add(new WHSDRegionsNode(childLabel, this));
            }
        } else {
            children = new ArrayList<>(0);
        }
        id = Integer.parseInt(label.id);
    }

    @Override
    public Integer getId() {
        return id;
    }

    /**
     * https://stackoverflow.com/questions/4129666/how-to-convert-hex-to-rgb-using-java
     * @param colorStr e.g. "#FFFFFF"
     * @return
     */
    public static int[] hex2Rgb(String colorStr) {
        return new int[]{
                Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorStr.substring( 5, 7 ), 16 ),255 };
    }

    //@Override
    //public Color getColor(AtlasNode node) {
    //    return hex2Rgb(((WHSDRegionsNode) node).properties.get("color"));
    //}

    @Override
    public int[] getColor() {
        return color;
    }

    @Override
    public Map<String, String> data() {
        return properties;
    }

    @Override
    public WHSDRegionsNode parent() {
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
