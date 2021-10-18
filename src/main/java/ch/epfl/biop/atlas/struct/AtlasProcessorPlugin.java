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
