/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2024 EPFL
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
package ch.epfl.biop.atlas.scijava;

import ch.epfl.biop.atlas.struct.Atlas;
import net.imagej.display.process.SingleInputPreprocessor;
import org.scijava.Priority;
import org.scijava.command.CommandService;
import org.scijava.module.ModuleService;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
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
        } catch (InterruptedException|ExecutionException e) {
            e.printStackTrace();
        }
        return a;
    }

}

