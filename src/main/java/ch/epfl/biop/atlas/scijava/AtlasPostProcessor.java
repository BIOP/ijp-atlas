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
package ch.epfl.biop.atlas.scijava;

import ch.epfl.biop.atlas.struct.Atlas;
import org.scijava.Priority;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.service.SourceService;

/**
 * Enables Atlas to be added to the object service when declared as an output
 * of a command
 */

@Plugin(type = PostprocessorPlugin.class, priority = Priority.VERY_LOW - 1)
public class AtlasPostProcessor extends AbstractPostprocessorPlugin {

	@Parameter
	ObjectService os;

	@Parameter
	SourceService source_service;
	
	@Override
	public void process(Module module) {
		
		module.getInfo().outputs().forEach(output -> {
			if ((output.getGenericType()== Atlas.class)&&(output.isOutput())) {
				final String name = output.getName();
				Atlas ba = (Atlas) module.getOutput(name);
				if (ba == null) {
					System.err.println("No atlas was returned - the open atlas command did not work as expected.");
				} else {
					if (!os.getObjects(Atlas.class).contains(ba)) { // Avoids double addition // TODO : avoid putting multiple times the same atlas
						os.addObject(ba);
						ba.getMap().getStructuralImages().forEach((key, source) -> source_service.register(source));
						source_service.register(ba.getMap().getLabelImage());
					}
				}
			}
		});
	}

}
