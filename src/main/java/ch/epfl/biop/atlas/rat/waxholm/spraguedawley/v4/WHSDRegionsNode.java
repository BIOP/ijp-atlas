package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4;

import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.ilfparser.Label;
import ch.epfl.biop.atlas.struct.AtlasNode;
import org.scijava.util.TreeNode;

import java.util.*;

public class WHSDRegionsNode implements AtlasNode {
    final Label label;
    final WHSDRegionsNode parent;
    final Map<String, String> properties;
    final List<TreeNode<?>> children;
    final Integer id;

    public static String toStringKey = "abbreviation";

    public WHSDRegionsNode(Label label, WHSDRegionsNode parent) {
        this.label = label;
        this.parent = parent;
        Map<String, String> mutableMap = new HashMap<>();
        mutableMap.put("id", label.id);
        mutableMap.put("abbreviation", label.abbreviation);
        mutableMap.put("name", label.name);
        mutableMap.put("color", label.color);
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

    @Override
    public Map<String, String> data() {
        return properties;
    }

    @Override
    public WHSDRegionsNode parent() {
        return parent;
    }

    @Override
    public void setParent(TreeNode<?> parent) {
        // Done in the constructor
        throw new UnsupportedOperationException("Cannot set parent, it is already set");
    }

    @Override
    public List<TreeNode<?>> children() {
        return children;
    }

    @Override
    public String toString() {
        return data().get(toStringKey);
    }

}
