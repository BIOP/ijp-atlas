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
    final int[] color;

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
