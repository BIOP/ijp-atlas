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

import ch.epfl.biop.atlas.struct.AtlasNode;
import com.google.gson.Gson;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BrainGlobeHelper {

    public static AtlasNode buildTreeAndGetRoot(String jsonStructuresFilePath) throws Exception {
        try(Reader r = Files.newBufferedReader(Paths.get(jsonStructuresFilePath))) {
            BrainGlobeStructures.Entry[] entries = new Gson().fromJson(r, BrainGlobeStructures.Entry[].class);
            Map<Integer, BrainGlobeStructures.Entry> idToEntry = new HashMap<>();
            Map<Integer, BrainGlobeNode> idToNode = new HashMap<>();
            Set<Integer> remainingIds = new HashSet<>();
            for (BrainGlobeStructures.Entry e : entries) {
                idToEntry.put(e.id,e);
                remainingIds.add(e.id);
            }

            // Pff... what an annoying way to construct a tree structure
            while (remainingIds.size()>0) {
                Integer currentId = remainingIds.stream().findAny().get();
                BrainGlobeNode node = new BrainGlobeNode(currentId,
                        idToEntry,
                        idToNode,
                        remainingIds);
            }

            BrainGlobeNode root = null;
            for (BrainGlobeNode node : idToNode.values()) {
                if (node.parent==null) {
                    if (root!=null) {
                        System.err.println("Error!! Multiple root ontology found: "+root.id+" and "+node.id);
                    } else {
                        root = node;
                    }
                }
            }

            if (root==null) {
                System.err.println("Error: root node of atlas ontology not found!");
            }
            return root;
        }
    }

    public static class BrainGlobeNode implements AtlasNode {
        final Integer id;
        final Map<Integer, BrainGlobeStructures.Entry> idToEntry;
        final int[] color;

        final Map<String,String> data = new HashMap<>(2);

        final AtlasNode parent;

        final List<BrainGlobeNode> children = new ArrayList<>();

        public BrainGlobeNode(Integer currentId,
                              Map<Integer, BrainGlobeStructures.Entry> idToEntry,
                              Map<Integer, BrainGlobeNode> idToNode,
                              Set<Integer> remainingIds) {
            id = currentId;
            BrainGlobeStructures.Entry entry = idToEntry.get(id);
            color = new int[]{entry.rgb_triplet[0],entry.rgb_triplet[1],entry.rgb_triplet[2],255};//entry.rgb_triplet.clone();
            data.put("name", entry.name);
            data.put("acronym", entry.acronym);
            data.put("id", Integer.toString(entry.id));

            idToNode.put(id,this);
            remainingIds.remove(id);
            this.idToEntry = idToEntry;

            if (entry.structure_id_path.length<=1) {
                // This is the root, nothing to be done here
                this.parent = null;
            } else {
                //System.out.println("entry.structure_id_path.length = "+entry.structure_id_path.length);
                int parent = entry.structure_id_path[entry.structure_id_path.length - 2];
                AtlasNode parentNode;
                if (!idToNode.containsKey(parent)) { // Make sure to create the node
                    new BrainGlobeNode(parent,
                            idToEntry,idToNode,remainingIds);
                }
                parentNode = idToNode.get(parent);
                this.parent = parentNode;
                List childrenList = this.parent.children();
                childrenList.add(this); // Hmpff!
            }
        }


        @Override
        public Integer getId() {
            return id;
        }

        @Override
        public int[] getColor() {
            return color;
        }

        @Override
        public Map<String, String> data() {
            return data;
        }

        @Override
        public AtlasNode parent() {
            return parent;
        }

        @Override
        public List<? extends AtlasNode> children() {
            return children;
        }

        public String toString() {
            return data.get("acronym");
        }
    }


}
