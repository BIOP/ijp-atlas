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
package ch.epfl.biop.atlas.mouse.allen.ccfv3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AllenOntologyJson {

    protected static Logger logger = LoggerFactory.getLogger(AllenOntologyJson.class);

    boolean success;
    int id;
    int start_row;
    int num_rows;
    int total_rows;
    List<AllenBrainRegion> msg;

    public class AllenBrainRegion {
        int id;
        int atlas_id;
        int ontology_id;
        String acronym;
        String name;
        String color_hex_triplet;
        int graph_order;
        int st_level;
        int hemisphere_id;
        int parent_structure_id;
        List<AllenBrainRegion> children;
    }

}
