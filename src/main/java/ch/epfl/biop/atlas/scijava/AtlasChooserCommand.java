package ch.epfl.biop.atlas.scijava;

import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p1.WaxholmSpragueDawleyRatV4p1Atlas;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p1.command.WaxholmSpragueDawleyRatV4p1Command;
import ch.epfl.biop.atlas.struct.Atlas;
import org.scijava.ItemIO;
import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Plugin(type = DynamicCommand.class, menuPath = "Plugins>BIOP>Atlas>Open Atlas", initializer = "init")
public class AtlasChooserCommand extends DynamicCommand {
    @Parameter
    ObjectService os;

    @Parameter
    CommandService cmd;

    @Parameter(label = "Choose an atlas", persist = false)
    String choice = "-";

    @Parameter(type = ItemIO.OUTPUT)
    Atlas atlas = null;
    @Override
    public void run() {
        List<Atlas> openedAtlases = os.getObjects(Atlas.class);

        for (Atlas a: openedAtlases) {
            //System.out.println(a.getName()+" vs "+choice);
            if (a.getName().equals(choice)) {
                //System.out.println("Atlas already opened, giving it back");
                atlas = a; // Atlas already opened
                return;
            }
        }

        try {
            switch (choice) {
                case WaxholmSpragueDawleyRatV4p1Atlas.atlasName:
                    atlas = (Atlas) cmd.run(WaxholmSpragueDawleyRatV4p1Command.class, true).get().getOutput("ba");
                    break;
                case AllenBrainAdultMouseAtlasCCF2017v3p1Command.atlasName:
                    atlas = (Atlas) cmd.run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");
                    break;
                default:
                    if (extraAtlases.containsKey(choice)) {
                        atlas = extraAtlases.get(choice).get();
                    } else {
                        System.err.println("Unrecognized atlas named " + choice);
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    static Map<String, Supplier<Atlas>> extraAtlases = new LinkedHashMap<>();

    public static void registerAtlas(String name, Supplier<Atlas> supplier) {
        if (extraAtlases.containsKey(name)) {
            System.err.println("Conflict : an atlas named "+name+" already exists. It will be overriden by the new one");
        } else {
            System.out.println("Adding "+name+" atlas");
        }
        extraAtlases.put(name, supplier);
    }

    protected void init() {
        final ArrayList<String> choices = new ArrayList<>();
        for (final Map.Entry<String, Supplier<Atlas>> entry : extraAtlases.entrySet()) {
            System.out.println(entry.getKey());
            choices.add(entry.getKey());
        }
        Collections.sort(choices);
        choices.add(0, WaxholmSpragueDawleyRatV4p1Atlas.atlasName);
        choices.add(0, AllenBrainAdultMouseAtlasCCF2017v3p1Command.atlasName);
        final MutableModuleItem<String> input = getInfo().getMutableInput("choice",
                String.class);
        input.setChoices(choices);
        input.setValue(this, choices.get(0));
    }
}
