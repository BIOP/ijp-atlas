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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2;

import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4.WaxholmSpragueDawleyRatV4Ontology;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasOntology;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WaxholmSpragueDawleyRatV4p2Atlas implements Atlas {

    WaxholmSpragueDawleyRatV4p2Map map;
    WaxholmSpragueDawleyRatV4Ontology ontology;

    @Override
    public AtlasMap getMap() {
        return map;
    }

    @Override
    public AtlasOntology getOntology() {
        return ontology;
    }

    @Override
    public void initialize(URL mapURL, URL ontologyURL) throws Exception{
        ontology = new WaxholmSpragueDawleyRatV4Ontology();
        ontology.setDataSource(ontologyURL);

        ontology.initialize();
        map = new WaxholmSpragueDawleyRatV4p2Map();
        map.setDataSource(mapURL);
        map.initialize(this.toString());
    }

    //https://www.nitrc.org/citation/?group_id=1081
    @Override
    public List<String> getDOIs() {
        List<String> dois = new ArrayList<>();
        dois.add("10.1016/j.neuroimage.2014.04.001");
        dois.add("10.1016/j.neuroimage.2014.10.017");
        dois.add("10.1016/j.neuroimage.2014.12.080");
        dois.add("10.1016/j.neuroimage.2019.05.016");
        return dois;
    }

    @Override
    public String getURL() {
        return "https://www.nitrc.org/projects/whs-sd-atlas";
    }

    final public static String atlasName = "Rat - Waxholm Sprague Dawley V4p2";

    @Override
    public String getName() {
        return atlasName;
    }

    @Override
    public String toString() {
        return getName();
    }


}
