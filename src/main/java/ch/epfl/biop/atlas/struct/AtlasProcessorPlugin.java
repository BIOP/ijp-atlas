/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2022 EPFL
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

import org.scijava.Priority;
import org.scijava.module.Module;
import org.scijava.module.process.AbstractPostprocessorPlugin;
import org.scijava.module.process.PostprocessorPlugin;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 * Enables Atlas to be added to the object service when declared as an output
 * of a command
 */

@Plugin(type = PostprocessorPlugin.class, priority = Priority.VERY_LOW - 1)
public class AtlasProcessorPlugin extends AbstractPostprocessorPlugin {

	@Parameter(required = false)
	private UIService ui;
	
	@Parameter
	ObjectService os;
	
	@Override
	public void process(Module module) {
		if (ui == null) {
			// no UIService available for displaying results
			return;
		} else {

		}
		
		module.getInfo().outputs().forEach(output -> {
			if ((output.getGenericType()== Atlas.class)&&(output.isOutput())) {
				final String name = output.getName();
				Atlas ba = (Atlas) module.getOutput(name);
				if (!os.getObjects(Atlas.class).contains(ba)) { // Avoids double addition // TODO : avoid putting multiple times the same atlas
					os.addObject(ba);
				}
				//ba.runOnClose(() -> os.removeObject(ba)); // removes the object when the atlas window is closed
			}
		});
	}

}
