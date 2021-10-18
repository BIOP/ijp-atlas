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
