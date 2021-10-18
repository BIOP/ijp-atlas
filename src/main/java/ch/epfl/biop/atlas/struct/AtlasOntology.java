package ch.epfl.biop.atlas.struct;

import java.awt.*;
import java.net.URL;

public interface AtlasOntology {

	String getName();

	void initialize() throws Exception;

	void setDataSource(URL dataSource);

	URL getDataSource();

	AtlasNode getRoot();

	Color getColor(AtlasNode node);

	AtlasNode getNodeFromId(int id);

}
