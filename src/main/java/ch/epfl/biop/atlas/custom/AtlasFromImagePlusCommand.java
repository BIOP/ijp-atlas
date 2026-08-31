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
package ch.epfl.biop.atlas.custom;

import ch.epfl.biop.atlas.scijava.AtlasChooserCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import ij.ImagePlus;
import org.scijava.command.Command;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type = Command.class,
        menuPath = "Plugins>BIOP>Atlas>Create Atlas from Images",
        description = "A simple way to create an atlas.")
public class AtlasFromImagePlusCommand implements Command {

    @Parameter
    String atlas_name;

    @Parameter
    ImagePlus structural_images;

    @Parameter(label = "Label Image")
    ImagePlus label_image;

    @Parameter
    Double atlas_precision_mm;

    @Parameter
    ObjectService os;

    @Override
    public void run() {
        Atlas atlas = AtlasFromSourcesHelper.fromImagePlus(atlas_name, structural_images, label_image, atlas_precision_mm);
        try {
            atlas.initialize(null, null);
            atlas.getOntology().initialize();
            os.addObject(atlas, atlas_name);
            AtlasChooserCommand.registerAtlas(atlas.getName(), () -> atlas);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
