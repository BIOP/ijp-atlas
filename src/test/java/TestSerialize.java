import ch.epfl.biop.atlas.mouse.allen.ccfv3.command.AllenBrainAdultMouseAtlasCCF2017Command;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4.command.WaxholmSpragueDawleyRatV4Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import net.imagej.ImageJ;

public class TestSerialize {
    public static void main(String[] args) throws Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        Atlas mouse_atlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017Command.class, true).get().getOutput("ba");

        AtlasHelper
                .saveOntologyToJsonFile(
                        mouse_atlas.getOntology(),
                        "src/test/resources/ontology_ccfv3_output.json");

        Atlas rat_atlas = (Atlas) ij.command().run(WaxholmSpragueDawleyRatV4Command.class, true).get().getOutput("ba");

        AtlasHelper
                .saveOntologyToJsonFile(
                        rat_atlas.getOntology(),
                        "src/test/resources/ontology_ratv4_output.json");

        AtlasOntology ontology = AtlasHelper.openOntologyFromJsonFile("src/test/resources/ontology_ccfv3_output.json");

        AtlasHelper
                .saveOntologyToJsonFile(
                        ontology,
                        "src/test/resources/ontology_ccfv3_output_resaved.json");
    }


}
