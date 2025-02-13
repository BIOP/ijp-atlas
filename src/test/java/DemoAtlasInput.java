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
import ch.epfl.biop.atlas.scijava.AtlasChooserCommand;
import ch.epfl.biop.atlas.scijava.AtlasPreprocessor;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.AtlasMap;
import ch.epfl.biop.atlas.struct.AtlasOntology;
import ij.IJ;
import net.imagej.ImageJ;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.net.URL;
import java.util.List;

@Plugin(type = Command.class)
public class DemoAtlasInput implements Command {

    @Parameter
    Atlas atlas;
    @Override
    public void run() {
        System.out.println("Atlas opened: "+atlas.getName());
    }

    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        AtlasChooserCommand.registerAtlas("Fish", () -> new Atlas() {
            @Override
            public AtlasMap getMap() {
                return null;
            }

            @Override
            public AtlasOntology getOntology() {
                return null;
            }

            @Override
            public void initialize(URL mapURL, URL ontologyURL) {

            }

            @Override
            public List<String> getDOIs() {
                return null;
            }

            @Override
            public String getURL() {
                return null;
            }

            @Override
            public String getName() {
                return "Fish";
            }
        });
        ij.command().run(DemoAtlasInput.class,true);
    }
}
