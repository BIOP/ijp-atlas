/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2025 EPFL
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
package ch.epfl.biop.atlas.struct;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An {@link Atlas} that combines a principal atlas with additional atlases.
 * <p>
 * The principal atlas provides the ontology, label image, coordinates,
 * and all structural conventions. Additional atlases contribute their
 * structural image channels as extra channels in the composite map.
 * <p>
 * All atlases must be initialized before being passed to this constructor.
 * <p>
 * Example usage:
 * <pre>
 *   Atlas composite = new CompositeAtlas(allenAtlas, brainglobeAtlas1, brainglobeAtlas2);
 * </pre>
 */
public class CompositeAtlas implements Atlas {

	private final Atlas principal;
	private final List<Atlas> additionalAtlases;
	private final CompositeAtlasMap compositeMap;

	public CompositeAtlas(Atlas principal, Atlas... additionalAtlases) {
		this(principal, Arrays.asList(additionalAtlases));
	}

	public CompositeAtlas(Atlas principal, List<Atlas> additionalAtlases) {
		this.principal = principal;
		this.additionalAtlases = new ArrayList<>(additionalAtlases);
		this.compositeMap = new CompositeAtlasMap(
				principal.getMap(),
				additionalAtlases.stream()
						.map(Atlas::getMap)
						.collect(Collectors.toList())
		);
	}

	@Override
	public AtlasMap getMap() {
		return compositeMap;
	}

	@Override
	public AtlasOntology getOntology() {
		return principal.getOntology();
	}

	@Override
	public void initialize(URL mapURL, URL ontologyURL) throws Exception {
		throw new UnsupportedOperationException(
				"CompositeAtlas cannot be initialized via URLs. " +
				"All constituent atlases must be initialized before constructing the composite.");
	}

	@Override
	public List<String> getDOIs() {
		List<String> allDois = new ArrayList<>(principal.getDOIs());
		for (Atlas additional : additionalAtlases) {
			for (String doi : additional.getDOIs()) {
				if (!allDois.contains(doi)) {
					allDois.add(doi);
				}
			}
		}
		return allDois;
	}

	@Override
	public String getURL() {
		return principal.getURL();
	}

	@Override
	public String getName() {
		return principal.getName();
	}

	public Atlas getPrincipalAtlas() {
		return principal;
	}

	public List<Atlas> getAdditionalAtlases() {
		return new ArrayList<>(additionalAtlases);
	}

	@Override
	public String toString() {
		return getName();
	}
}