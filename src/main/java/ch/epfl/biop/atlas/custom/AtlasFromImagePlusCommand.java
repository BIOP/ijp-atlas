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
package ch.epfl.biop.atlas.custom;

import ch.epfl.biop.atlas.scijava.AtlasChooserCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasHelper;
import com.google.gson.GsonBuilder;
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
