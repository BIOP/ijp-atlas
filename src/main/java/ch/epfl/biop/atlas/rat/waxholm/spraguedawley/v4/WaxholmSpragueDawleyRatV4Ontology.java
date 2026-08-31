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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4;

import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.ilfparser.ILF;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;

import javax.xml.bind.JAXBContext;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.ilfparser.Label;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.net.URL;
import java.util.Map;

public class WaxholmSpragueDawleyRatV4Ontology implements AtlasOntology {

    URL dataSource;

    AtlasNode root;

    transient Map<Integer, AtlasNode> idToAtlasNodeMap;

    @Override
    public String getName() {
        return  "waxholm_sprague_dawley_rat_v4";
    }

    @Override
    public void initialize() throws Exception {
        // called before or after setdatasource ?

        try (Reader fileReader = new BufferedReader(
                new FileReader(new File(getDataSource().toURI())))){

            JAXBContext context = JAXBContext.newInstance(ILF.class);
            ILF ilf = (ILF) context.createUnmarshaller()
                    .unmarshal(fileReader);

            // No root, wonderful! Let's create a fake one
            Label fakeRoot = new Label();
            fakeRoot.id = "997"; // because why not ?
            fakeRoot.labels  = new Label[3];
            fakeRoot.labels[0] = ilf.structure.labels[0];
            fakeRoot.labels[1] = ilf.structure.labels[1];
            fakeRoot.labels[2] = ilf.structure.labels[2];
            fakeRoot.name = "root";
            fakeRoot.abbreviation = "root";
            fakeRoot.color = "#32a852";

            root = new WHSDRegionsNode(fakeRoot, null);
            idToAtlasNodeMap = AtlasHelper.buildIdToAtlasNodeMap(root);
        }
    }

    @Override
    public void setDataSource(URL dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public URL getDataSource() {
        return dataSource;
    }

    @Override
    public AtlasNode getRoot() {
        return root;
    }

    @Override
    public AtlasNode getNodeFromId(int id) {
        return idToAtlasNodeMap.get(id);
    }

    String namingProperty = "abbreviation";

    @Override
    public String getNamingProperty() {
        return namingProperty;
    }

    @Override
    public void setNamingProperty(String namingProperty) {
        this.namingProperty = namingProperty;
    }

}
