package ch.epfl.biop.atlas.scijava;

import bdv.util.BdvHandle;
import ch.epfl.biop.atlas.struct.Atlas;
import net.imagej.display.process.SingleInputPreprocessor;
import org.scijava.Priority;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.fiji.bdvpg.scijava.services.SourceAndConverterBdvDisplayService;

import java.util.concurrent.ExecutionException;

/**
 * Fills single, unresolved module inputs with the active {@link Atlas},
 * <em>or a newly created one if none</em>.
 *
 * @author Nicolas Chiaruttini
 */
@SuppressWarnings("unused")
@Plugin(type = PreprocessorPlugin.class, priority = Priority.VERY_HIGH)
public class AtlasPreprocessor extends SingleInputPreprocessor<Atlas> {

    @Parameter
    CommandService commandService;

    @Parameter
    ModuleService ms;

    public AtlasPreprocessor() {
        super(Atlas.class);
    }

    // -- SingleInputProcessor methods --

    @Override
    public Atlas getValue() {
        Atlas a = null;
        try {
            a = (Atlas) ms.run(commandService.getCommand(AtlasChooserCommand.class), true).get().getOutput("atlas");
            //a = (Atlas) commandService.run(AtlasChooserCommand.class, true).get().getOutput("atlas");
        } catch (InterruptedException|ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println("a:"+a.getName());
        return a;
    }

}

