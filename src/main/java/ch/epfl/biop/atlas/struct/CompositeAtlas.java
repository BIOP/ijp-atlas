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
package ch.epfl.biop.atlas.struct;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	final String name;

	public CompositeAtlas(Atlas principal, List<Atlas> additionalAtlases) {
		this.principal = principal;
		this.additionalAtlases = new ArrayList<>(additionalAtlases);
		this.compositeMap = new CompositeAtlasMap(principal, this.additionalAtlases);
		StringBuilder nameBuilder = new StringBuilder();
		nameBuilder.append(principal.getName());
		additionalAtlases.forEach((atlas) -> nameBuilder.append("+").append(atlas.getName()));
		this.name = nameBuilder.toString();
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
	public void initialize(URL mapURL, URL ontologyURL) {
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
		return name;
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
