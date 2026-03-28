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
package ch.epfl.biop.atlas.scijava;

import ch.epfl.biop.atlas.brainglobe.BrainGlobeAppose;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAtlas;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr.command.AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.WaxholmSpragueDawleyRatV4p2Atlas;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.command.WaxholmSpragueDawleyRatV4p2Command;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2asr.command.WaxholmSpragueDawleyRatV4p2ASRCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import org.scijava.Context;
import org.scijava.ItemIO;

import org.scijava.command.CommandService;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Plugin(type = DynamicCommand.class, menuPath = "Plugins>BIOP>Atlas>Open Atlas",
        initializer = "init",
        iconPath = "/graphics/brainglobe.png")
public class AtlasChooserCommand extends DynamicCommand {

    @Parameter
    ObjectService os;

    @Parameter
    CommandService cmd;

    @Parameter(label = "Choose an atlas", callback = "checkBrainGlobe")
    String choice = "-";

    @Parameter(type = ItemIO.OUTPUT)
    Atlas atlas = null;

    @Parameter
    Context ctx;

    @Override
    public void run() {
        List<Atlas> openedAtlases = os.getObjects(Atlas.class);

        for (Atlas a: openedAtlases) {
            if (a.getName().equals(choice)) {
                atlas = a; // Atlas already opened
                return;
            }
        }

        try {
            switch (choice) {
                case WaxholmSpragueDawleyRatV4p2Atlas.atlasName:
                    atlas = (Atlas) cmd.run(WaxholmSpragueDawleyRatV4p2Command.class, true).get().getOutput("ba");
                    break;
                case AllenBrainAdultMouseAtlasCCF2017v3p1Command.atlasName:
                    atlas = (Atlas) cmd.run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");
                    break;
                case AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.atlasName:
                    atlas = (Atlas) cmd.run(AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.class, true).get().getOutput("ba");
                    break;
                case WaxholmSpragueDawleyRatV4p2ASRCommand.atlasName:
                    atlas = (Atlas) cmd.run(WaxholmSpragueDawleyRatV4p2ASRCommand.class, true).get().getOutput("ba");
                    break;
                default:
                    if (!extraAtlases.containsKey(choice)) {
                        if (!brainGlobeRegistered) {
                            registerBrainGlobeAtlases(ctx);
                            init();
                        }
                    }

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

    private static volatile boolean brainGlobeRegistered = false;

    public static void registerAtlas(String name, Supplier<Atlas> supplier) {
        if (extraAtlases.containsKey(name)) {
            System.err.println("Conflict : an atlas named "+name+" already exists. It will be overriden by the new one");
        }
        extraAtlases.put(name, supplier);
    }

    /**
     * Registers all available BrainGlobe atlases as extra atlases.
     * Called once; if env build fails, silently skips and won't retry this session.
     */
    private static synchronized void registerBrainGlobeAtlases(Context ctx) {
        if (brainGlobeRegistered) return;
        brainGlobeRegistered = true;

        BrainGlobeAppose.setContext(ctx);

        List<String> bgAtlases = BrainGlobeAppose.getAvailableAtlasNames();
        for (String bgName : bgAtlases) {
            // Prefix with "BrainGlobe: " to distinguish from built-in atlases
            extraAtlases.put(bgName, () -> {
                try {
                    BrainGlobeAtlas atlas = new BrainGlobeAtlas(bgName);
                    atlas.initialize(null, null);
                    return atlas;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load BrainGlobe atlas: " + bgName, e);
                }
            });
        }
        if (!bgAtlases.isEmpty()) {
            System.out.println("Registered " + bgAtlases.size() + " BrainGlobe atlases");
        }
    }

    protected void init() {

        String iniValue = this.choice;

        final ArrayList<String> choices = new ArrayList<>();
        for (final Map.Entry<String, Supplier<Atlas>> entry : extraAtlases.entrySet()) {
            System.out.println(entry.getKey());
            choices.add(entry.getKey());
        }
        Collections.sort(choices);
        choices.add(0, WaxholmSpragueDawleyRatV4p2Atlas.atlasName);
        choices.add(0, WaxholmSpragueDawleyRatV4p2ASRCommand.atlasName);
        choices.add(0, AllenBrainAdultMouseAtlasCCF2017v3p1Command.atlasName);
        choices.add(0, AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.atlasName);

        if (!brainGlobeRegistered) {
            choices.add(BRAINGLOBE_OPTION);
        }

        final MutableModuleItem<String> input = getInfo().getMutableInput("choice",
                String.class);
        input.setChoices(choices);
        input.setValue(this, iniValue);
    }

    final static String BRAINGLOBE_OPTION = "Get BrainGlobe Atlases (web)...";

    void checkBrainGlobe() {
        if (this.choice.equals(BRAINGLOBE_OPTION)) {
            if (!brainGlobeRegistered) {
                UIService uiService = ctx.getService(UIService.class);
                boolean headless = (uiService == null) || uiService.isHeadless();

                JDialog waitDialog = null;
                if (!headless) {
                    waitDialog = new JDialog((java.awt.Frame) null, "Loading BrainGlobe Atlases", true);
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

                    JLabel infoLabel = new JLabel("<html>"
                            + "<h2>Loading BrainGlobe Atlases (v"+BrainGlobeAppose.BG_VERSION+")</h2>"
                            + "<p><img src='" + AtlasChooserCommand.class.getClassLoader().getResource("graphics/brainglobe.png") + "' width='80' height='80'></p>"
                            + "<p>For more information, visit <a href='https://brainglobe.info'>https://brainglobe.info</a></p>"
                            + "</html>", SwingConstants.CENTER);
                    infoLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
                    panel.add(infoLabel);

                    panel.add(Box.createVerticalStrut(15));

                    ImageIcon loadingIcon = new ImageIcon(AtlasChooserCommand.class.getClassLoader().getResource("graphics/loading.gif"));
                    loadingIcon.setImage(loadingIcon.getImage().getScaledInstance(64, 64, java.awt.Image.SCALE_DEFAULT));
                    JLabel waitLabel = new JLabel("Loading BrainGlobe atlases, please wait...", loadingIcon, SwingConstants.CENTER);
                    waitLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
                    panel.add(waitLabel);

                    waitDialog.getContentPane().add(panel);
                    waitDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
                    waitDialog.pack();
                    waitDialog.setLocationRelativeTo(null);

                    // SwingWorker runs registration in background;
                    // disposing the modal dialog unblocks setVisible(true) below
                    final JDialog dlg = waitDialog;
                    final Context context = ctx;
                    new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() {
                            registerBrainGlobeAtlases(context);
                            return null;
                        }
                        @Override
                        protected void done() {
                            dlg.dispose();
                        }
                    }.execute();

                    waitDialog.setVisible(true); // blocks here until worker disposes the dialog
                } else {
                    registerBrainGlobeAtlases(ctx);
                }

                init();
            }
        }
    }

}
