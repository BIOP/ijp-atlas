package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4;

import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.ilfparser.ILF;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import ch.epfl.biop.atlas.struct.AtlasNode;
import ch.epfl.biop.atlas.struct.AtlasOntology;

import javax.xml.bind.JAXBContext;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.ilfparser.Label;

import java.awt.*;
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

    /**
     * https://stackoverflow.com/questions/4129666/how-to-convert-hex-to-rgb-using-java
     * @param colorStr e.g. "#FFFFFF"
     * @return
     */
    public static Color hex2Rgb(String colorStr) {
        return new Color(
                Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }

    @Override
    public Color getColor(AtlasNode node) {
        return hex2Rgb(((WHSDRegionsNode) node).properties.get("color"));
    }

    @Override
    public AtlasNode getNodeFromId(int id) {
        return idToAtlasNodeMap.get(id);
    }

}
