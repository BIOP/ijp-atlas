package ch.epfl.biop.atlas.struct;

import java.util.List;
import java.util.Map;

public interface AtlasNode {
    Integer getId();

    int[] getColor();

    /** Gets the data associated with the node. */
    Map<String, String> data();

    /** Gets the parent of this node. */
    AtlasNode parent();

    /**
     * Gets the node's children. If this list is mutated, the children will be
     * affected accordingly. It is the responsibility of the caller to ensure
     * continued integrity, particularly of parent linkages.
     */
    List<? extends AtlasNode> children();


}
