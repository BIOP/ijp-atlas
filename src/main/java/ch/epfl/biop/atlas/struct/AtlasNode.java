package ch.epfl.biop.atlas.struct;

import org.scijava.util.TreeNode;

import java.util.Map;

public interface AtlasNode extends TreeNode<Map<String, String>> {
    Integer getId();
}
