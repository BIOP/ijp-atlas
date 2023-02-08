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
