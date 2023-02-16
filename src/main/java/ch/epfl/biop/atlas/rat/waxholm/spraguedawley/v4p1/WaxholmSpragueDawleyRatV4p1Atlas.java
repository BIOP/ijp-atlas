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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p1;

import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4.WaxholmSpragueDawleyRatV4Map;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4.WaxholmSpragueDawleyRatV4Ontology;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasOntology;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WaxholmSpragueDawleyRatV4p1Atlas implements Atlas {

    WaxholmSpragueDawleyRatV4p1Map map;
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
        map = new WaxholmSpragueDawleyRatV4p1Map();
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

    final public static String atlasName = "Rat - Waxholm Sprague Dawley V4p1";
    @Override
    public String getName() {
        return atlasName;
    }

    @Override
    public String toString() {
        return getName();
    }


}
