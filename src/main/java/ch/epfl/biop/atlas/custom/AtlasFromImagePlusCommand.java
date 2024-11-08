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

    @Parameter(label = "Label Image (optional)", required = false)
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

            //AtlasHelper.saveOntologyToJsonFile(atlas.getOntology(), ontology.getAbsolutePath());

            System.out.println(
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create()
                    .toJson(new AtlasHelper.SerializableOntology(atlas.getOntology())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
