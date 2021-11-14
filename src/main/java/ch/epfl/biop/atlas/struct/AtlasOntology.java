package ch.epfl.biop.atlas.struct;

import java.net.URL;

public interface AtlasOntology {

	String getName();

	void initialize() throws Exception;

	void setDataSource(URL dataSource);

	URL getDataSource();

	AtlasNode getRoot();

	AtlasNode getNodeFromId(int id);

	String getNamingProperty();

	void setNamingProperty(String namingProperty);

}
