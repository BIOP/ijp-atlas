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

