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

import ch.epfl.biop.atlas.brainglobe.BrainGlobeAppose;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAtlasId;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeLocalInventory;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Plugin(type = Command.class, menuPath = "Plugins>BIOP>Atlas>List BrainGlobe Atlases",
        description = "Lists every atlas published by BrainGlobe (the latest version of each), and the ones already "
                + "downloaded on this computer. Any of them opens with 'Open Atlas', with its name and version as the "
                + "choice (e.g. allen_mouse_25um@3.0): one not downloaded yet is downloaded first (minutes, up to a few GB). "
                + "The first listing of a Fiji session needs BrainGlobe's Python environment, which takes minutes to build "
                + "the first time on a computer. Afterwards, 'Open Atlas' offers the whole list too.",
        iconPath = "/graphics/brainglobe.png")
public class BrainGlobeAtlasListCommand implements Command {

    @Parameter
    Context ctx;

    @Parameter(label = "Filter", required = false,
            description = "Only the atlases whose name contains all these words, separated by spaces (e.g. 'mouse', "
                    + "'rat 25um'). Empty for all.")
    String filter = "";

    @Parameter(type = ItemIO.OUTPUT, label = "Atlases",
            description = "A summary line, then one atlas per line: its name and version, followed by '(downloaded)' "
                    + "for the ones on this computer.")
    String atlases;

    @Override
    public void run() {
        AtlasChooserCommand.registerBrainGlobeAtlases(ctx); // Open Atlas then offers the downloads as well
        List<BrainGlobeAtlasId> catalogue = BrainGlobeAppose.getAvailableAtlasIds();
        Set<String> downloaded = BrainGlobeLocalInventory.listMaterialized().stream()
                .map(BrainGlobeAtlasId::toString).collect(Collectors.toCollection(TreeSet::new));
        if (catalogue.isEmpty()) {
            throw new IllegalStateException("The BrainGlobe catalogue could not be listed (no network, or BrainGlobe's "
                    + "Python environment could not be built: see the log). Downloaded on this computer: "
                    + (downloaded.isEmpty() ? "none" : String.join(", ", downloaded)));
        }

        // The latest version of each atlas, and the versions on disk, which may be older
        Set<String> all = new TreeSet<>(downloaded);
        catalogue.forEach(id -> all.add(id.toString()));
        List<String> words = Arrays.stream(filter == null ? new String[0] : filter.toLowerCase(Locale.ROOT).trim().split("\\s+"))
                .filter(w -> !w.isEmpty()).collect(Collectors.toList());
        List<String> lines = all.stream()
                .filter(name -> words.stream().allMatch(name.toLowerCase(Locale.ROOT)::contains))
                .map(name -> downloaded.contains(name) ? name + "   (downloaded)" : name)
                .collect(Collectors.toList());

        long nDownloaded = lines.stream().filter(line -> line.endsWith("(downloaded)")).count();
        String scope = words.isEmpty() ? "" : " matching '" + filter.trim() + "'";
        atlases = lines.size() + " BrainGlobe atlases" + scope + " (of " + all.size() + "), " + nDownloaded
                + " downloaded. Open one with 'Open Atlas', its name and version as the choice.\n"
                + String.join("\n", lines);
    }
}
