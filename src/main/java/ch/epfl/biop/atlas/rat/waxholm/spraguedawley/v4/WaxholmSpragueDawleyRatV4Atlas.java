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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4;

import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import ch.epfl.biop.atlas.struct.Atlas;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WaxholmSpragueDawleyRatV4Atlas implements Atlas {

    WaxholmSpragueDawleyRatV4Map map;
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
    public void initialize(URL mapURL, URL ontologyURL) {
        ontology = new WaxholmSpragueDawleyRatV4Ontology();
        ontology.setDataSource(ontologyURL);

        try {
            ontology.initialize();
            map = new WaxholmSpragueDawleyRatV4Map();
            map.setDataSource(mapURL);
            map.initialize(this.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    @Override
    public String getName() {
        return "Waxholm Sprague-Dawley Rat Atlas V4";
    }

    @Override
    public String toString() {
        return getName();
    }


}
