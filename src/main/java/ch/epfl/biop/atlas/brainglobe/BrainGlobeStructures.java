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
