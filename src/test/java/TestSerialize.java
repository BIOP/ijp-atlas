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

import ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr.command.AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.command.WaxholmSpragueDawleyRatV4p2Command;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import net.imagej.ImageJ;

public class TestSerialize {
    public static void main(String[] args) throws Exception{
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        Atlas mouse_atlas = (Atlas) ij.command().run(AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.class, true).get().getOutput("ba");

        AtlasHelper
                .saveOntologyToJsonFile(
                        mouse_atlas.getOntology(),
                        "src/test/resources/ontology_ccfv3_output.json");

        Atlas rat_atlas = (Atlas) ij.command().run(WaxholmSpragueDawleyRatV4p2Command.class, true).get().getOutput("ba");

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
