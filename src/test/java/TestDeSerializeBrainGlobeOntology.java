import ch.epfl.biop.atlas.brainglobe.BrainGlobeHelper;
import ch.epfl.biop.atlas.struct.AtlasNode;

public class TestDeSerializeBrainGlobeOntology {
    public static void main(String... args) throws Exception {
        AtlasNode root = BrainGlobeHelper.buildTreeAndGetRoot("src/test/resources/structures_brainglobe_example.json");
        System.out.println(root.getId()); // Should be 997
        System.out.println(root.children());
    }

}
