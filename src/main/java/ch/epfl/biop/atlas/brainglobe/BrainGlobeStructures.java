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
package ch.epfl.biop.atlas.brainglobe;

import java.util.List;


        /*"acronym":"TMv",
                "id":1,
                "name":"Tuberomammillary nucleus, ventral part",
                "structure_id_path":[
                997,
                8,
                343,
                1129,
                1097,
                467,
                331,
                557,
                1
                ],
                "rgb_triplet":[
                255,
                255,
                255
                ]*/

public class BrainGlobeStructures {
    List<Entry> entries;
    public static class Entry {
        String acronym;
        Integer id;
        String name;
        int[] structure_id_path;
        int[] rgb_triplet;
    }
}
