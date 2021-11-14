/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 EPFL
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
