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
package ch.epfl.biop.atlas.mouse.allen.ccfv3;

import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import com.google.gson.Gson;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AllenOntology implements AtlasOntology {

    URL dataSource;
    transient AllenOntologyJson ontology;
    transient AtlasNode root;
    transient Map<Integer, AtlasNode> idToAtlasNodeMap;

    public String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void initialize() throws Exception {
        // called before or after setdatasource ?

        try (Reader fileReader = new BufferedReader(
                new FileReader(new File(getDataSource().toURI())))){
            Gson gson = new Gson();
            ontology = gson.fromJson(fileReader, AllenOntologyJson.class);
        }
        root = new AllenBrainRegionsNode(ontology.msg.get(0), null);
        idToAtlasNodeMap = AtlasHelper.buildIdToAtlasNodeMap(root);
        Map<Integer, AtlasNode> moduloAdd = new HashMap<>();
        idToAtlasNodeMap.forEach((id,v) ->
            moduloAdd.put(id % 65000, v) // because the map is modulo 65000
        );
        idToAtlasNodeMap.putAll(moduloAdd);
    }

    @Override
    public void setDataSource(URL dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public URL getDataSource() {
        return this.dataSource;
    }

    @Override
    public AtlasNode getRoot() {
        return root;
    }



    @Override
    public AtlasNode getNodeFromId(int id) {
        return idToAtlasNodeMap.get(id);
    }

    String namingProperty = "acronym";

    @Override
    public String getNamingProperty() {
        return namingProperty;
    }

    @Override
    public void setNamingProperty(String namingProperty) {
        this.namingProperty = namingProperty;
    }

}
