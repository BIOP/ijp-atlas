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
            while (!remainingIds.isEmpty()) {
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
