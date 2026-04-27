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

import ch.epfl.biop.atlas.AtlasLocationHelper;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAppose;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAtlas;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1.command.AllenBrainAdultMouseAtlasCCF2017v3p1Command;
import ch.epfl.biop.atlas.mouse.allen.ccfv3p1asr.command.AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.WaxholmSpragueDawleyRatV4p2Atlas;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2.command.WaxholmSpragueDawleyRatV4p2Command;
import ch.epfl.biop.atlas.rat.waxholm.spraguedawley.v4p2asr.command.WaxholmSpragueDawleyRatV4p2ASRCommand;
import ch.epfl.biop.atlas.struct.Atlas;
import ch.epfl.biop.atlas.struct.CompositeAtlas;
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

@SuppressWarnings("CanBeFinal")
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

    @Parameter(label = "Additional atlases (comma-separated, optional)",
            description = "Comma-separated atlas names whose structural channels will be merged into the principal atlas via CompositeAtlas. Leave empty for a plain atlas.",
            required = false)
    String additionalAtlases = "";

    @Parameter(type = ItemIO.OUTPUT)
    Atlas atlas = null;

    @Parameter
    Context ctx;

    @Override
    public void run() {
        // Resolve the principal atlas
        Atlas principalAtlas = resolveAtlasByName(choice);
        if (principalAtlas == null) {
            System.err.println("Could not resolve principal atlas: " + choice);
            return;
        }

        // Parse and resolve additional atlases
        List<Atlas> extras = new ArrayList<>();
        if (additionalAtlases != null && !additionalAtlases.trim().isEmpty()) {
            String[] names = additionalAtlases.split(",");
            for (String rawName : names) {
                String name = rawName.trim();
                if (name.isEmpty()) continue;
                if (name.equals(choice)) {
                    System.err.println("Skipping additional atlas '" + name + "': same as principal atlas");
                    continue;
                }
                Atlas extra = resolveAtlasByName(name);
                if (extra != null) {
                    extras.add(extra);
                } else {
                    System.err.println("Could not resolve additional atlas: " + name);
                }
            }
        }

        if (extras.isEmpty()) {
            atlas = principalAtlas;
        } else {
            atlas = new CompositeAtlas(principalAtlas, extras);
        }
    }

    /**
     * Resolves an atlas by name: first checks ObjectService for an already-opened
     * atlas, then falls back to creating a new one via built-in commands or extra suppliers.
     */
    private Atlas resolveAtlasByName(String name) {
        // Check already-opened atlases first
        for (Atlas a : os.getObjects(Atlas.class)) {
            if (a.getName().equals(name)) {
                return a;
            }
        }

        try {
            switch (name) {
                case WaxholmSpragueDawleyRatV4p2Atlas.atlasName:
                    return (Atlas) cmd.run(WaxholmSpragueDawleyRatV4p2Command.class, true).get().getOutput("ba");
                case AllenBrainAdultMouseAtlasCCF2017v3p1Command.atlasName:
                    return (Atlas) cmd.run(AllenBrainAdultMouseAtlasCCF2017v3p1Command.class, true).get().getOutput("ba");
                case AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.atlasName:
                    return (Atlas) cmd.run(AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.class, true).get().getOutput("ba");
                case WaxholmSpragueDawleyRatV4p2ASRCommand.atlasName:
                    return (Atlas) cmd.run(WaxholmSpragueDawleyRatV4p2ASRCommand.class, true).get().getOutput("ba");
                default:
                    if (!extraAtlases.containsKey(name)) {
                        if (!brainGlobeRegistered) {
                            registerBrainGlobeAtlases(ctx);
                            init();
                        }
                    }

                    if (extraAtlases.containsKey(name)) {
                        return extraAtlases.get(name).get();
                    } else {
                        System.err.println("Unrecognized atlas named " + name);
                        return null;
                    }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
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

        AtlasLocationHelper.setContext(ctx);

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
        AtlasLocationHelper.setContext(ctx);

        final ArrayList<String> choices = new ArrayList<>();
        for (final Map.Entry<String, Supplier<Atlas>> entry : extraAtlases.entrySet()) {
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

    final static String BRAINGLOBE_OPTION = "Get BrainGlobe Atlases...";

    void checkBrainGlobe() {
        if (this.choice.equals(BRAINGLOBE_OPTION)) {
            if (!brainGlobeRegistered) {
                UIService uiService = ctx.getService(UIService.class);
                boolean headless = (uiService == null) || uiService.isHeadless();

                JDialog waitDialog;
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
