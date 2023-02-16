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
package ch.epfl.biop.atlas.struct;

import java.net.URL;
import java.util.List;

public interface Atlas {

    // An atlas is : an ontology and an xml hdf5 data source

    //--------------------------- Source
    // Sources contains the xml hdf5 label image, leaves only data
    // Source -> then several imaging modalities Different visualisation
    AtlasMap getMap();
    
    //--------------------------- Ontology
    AtlasOntology getOntology();

    void initialize(URL mapURL, URL ontologyURL) throws Exception;

    List<String> getDOIs();

    String getURL();

    String getName();
}
