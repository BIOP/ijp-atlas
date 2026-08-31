/*-
 * #%L
 * Repo containing a standard API for Atlases and some example ones
 * %%
 * Copyright (C) 2021 - 2026 EPFL
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
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAtlasId;
import ch.epfl.biop.atlas.brainglobe.BrainGlobeLocalInventory;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        String principalName = toAtlasName(choice);
        if (principalName == null) {
            // The separator row, or the BrainGlobe placeholder left unresolved
            System.err.println("No atlas selected");
            return;
        }
        if (!confirmDownloads(principalName)) {
            return;
        }

        // Resolve the principal atlas
        Atlas principalAtlas = resolveAtlasByName(principalName);
        if (principalAtlas == null) {
            System.err.println("Could not resolve principal atlas: " + principalName);
            return;
        }

        // Parse and resolve additional atlases
        List<Atlas> extras = new ArrayList<>();
        if (additionalAtlases != null && !additionalAtlases.trim().isEmpty()) {
            String[] names = additionalAtlases.split(",");
            for (String rawName : names) {
                String name = rawName.trim();
                if (name.isEmpty()) continue;
                if (name.equals(principalName)) {
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
     * Warns before anything has to be downloaded, and lets the user back out.
     * <p>
     * ABBA materializes an atlas in full rather than streaming it, so opening one
     * that is not yet on disk is a download of the whole brain — minutes, and a lot
     * of disk — rather than the instant operation every other entry in the dropdown
     * is. Picking a row in a list should not commit someone to that unannounced.
     * <p>
     * Headless callers are not prompted: a script that names an atlas has already
     * said what it wants.
     *
     * @return true if the run should go ahead
     */
    private boolean confirmDownloads(String principalName) {
        List<String> toDownload = new ArrayList<>();
        collectPendingDownload(principalName, toDownload);
        if (additionalAtlases != null) {
            for (String rawName : additionalAtlases.split(",")) {
                collectPendingDownload(toAtlasName(rawName.trim()), toDownload);
            }
        }
        if (toDownload.isEmpty()) {
            return true;
        }

        String message = "The following atlas" + (toDownload.size() > 1 ? "es have" : " has")
                + " not been downloaded yet:\n\n  " + String.join("\n  ", toDownload)
                + "\n\nABBA downloads the whole atlas before using it, rather than streaming it,\n"
                + "so this happens once and everything afterwards is fast and works offline.\n"
                + "The download can take several minutes and use a few GB of disk.\n\nContinue?";

        UIService uiService = ctx.getService(UIService.class);
        if (uiService == null || uiService.isHeadless()) {
            System.out.println(message);
            return true;
        }
        int answer = JOptionPane.showConfirmDialog(null, message, "Download atlas",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
        return answer == JOptionPane.OK_OPTION;
    }

    /** Adds {@code name} to {@code toDownload} if it is a BrainGlobe atlas not yet on disk */
    private static void collectPendingDownload(String name, List<String> toDownload) {
        if (name == null || name.isEmpty()) return;
        if (extraAtlases.containsKey(name)) return;
        if (!BrainGlobeAtlasId.hasVersion(name)) return; // built-in, or an error reported later
        try {
            if (!BrainGlobeLocalInventory.isMaterialized(BrainGlobeAtlasId.parse(name))) {
                toDownload.add(name);
            }
        } catch (IllegalArgumentException e) {
            // Malformed id: let the resolution step report it properly
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
                    // Atlases registered by other code (custom ImagePlus atlases, and
                    // anything a script has added) keep their own naming: the version
                    // requirement below applies to BrainGlobe names only.
                    if (extraAtlases.containsKey(name)) {
                        return extraAtlases.get(name).get();
                    }

                    return resolveBrainGlobeAtlas(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Resolves a BrainGlobe atlas, which must carry a version.
     * <p>
     * A bare name is refused rather than resolved to the latest version: atlas
     * versions renumber regions, so quietly picking one is how a dataset gets
     * reinterpreted against an ontology it was never aligned to. The message names
     * the versions that would work, so the fix is a copy-paste away.
     */
    private Atlas resolveBrainGlobeAtlas(String name) throws Exception {
        if (!BrainGlobeAtlasId.hasVersion(name)) {
            System.err.println(versionRequiredMessage(name));
            return null;
        }

        BrainGlobeAtlasId id;
        try {
            id = BrainGlobeAtlasId.parse(name);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return null;
        }

        // An atlas already materialized opens straight away — no catalogue, no
        // network, and no Python environment needed beyond the one that reads it.
        // Anything else has to be downloaded, which is what the catalogue is for.
        if (!BrainGlobeLocalInventory.isMaterialized(id)
                && !BrainGlobeAppose.getAvailableAtlasIds().contains(id)) {
            if (!BrainGlobeAppose.isBrainGlobeAvailable()) {
                System.err.println("Atlas '" + id + "' is not installed, and the BrainGlobe catalogue "
                        + "could not be reached to download it.");
                return null;
            }
            System.err.println("Unknown BrainGlobe atlas '" + id + "'. " + availableVersionsHint(id.getName()));
            return null;
        }

        BrainGlobeAtlas atlas = new BrainGlobeAtlas(id);
        atlas.initialize(null, null);
        return atlas;
    }

    /** Error text for a BrainGlobe atlas named without a version */
    private static String versionRequiredMessage(String bareName) {
        return "Atlas '" + bareName + "' has no version. BrainGlobe atlas versions change region ids, "
                + "so the version must be given explicitly, as in '" + bareName + "@3.1'. "
                + availableVersionsHint(bareName);
    }

    /**
     * Lists the versions of {@code bareName} that could be opened, installed ones first.
     * <p>
     * The catalogue is only consulted if it has already been fetched: building a
     * Python environment for minutes just to word an error message would be a poor
     * trade, and the installed versions are usually the useful half of the answer.
     */
    private static String availableVersionsHint(String bareName) {
        List<String> installed = new ArrayList<>();
        for (BrainGlobeAtlasId id : BrainGlobeLocalInventory.listMaterialized()) {
            if (id.getName().equals(bareName)) installed.add(id.toString());
        }
        List<String> downloadable = new ArrayList<>();
        if (BrainGlobeAppose.isBrainGlobeAvailable()) {
            for (BrainGlobeAtlasId id : BrainGlobeAppose.getAvailableAtlasIds()) {
                if (id.getName().equals(bareName) && !installed.contains(id.toString())) {
                    downloadable.add(id.toString());
                }
            }
        }
        if (installed.isEmpty() && downloadable.isEmpty()) {
            return "No version of it is installed.";
        }
        StringBuilder sb = new StringBuilder("Available: ");
        for (String id : installed) sb.append("\n  ").append(id).append("   (installed)");
        for (String id : downloadable) sb.append("\n  ").append(id).append("   (would be downloaded)");
        return sb.toString();
    }

    static Map<String, Supplier<Atlas>> extraAtlases = new LinkedHashMap<>();

    private static volatile boolean brainGlobeRegistered = false;

    /**
     * Registers an atlas under a name of the caller's choosing.
     * <p>
     * BrainGlobe atlases are not registered this way — they are enumerated from the
     * catalogue and from disk — so {@code @} is reserved and refused here, to keep it
     * meaning exactly one thing.
     */
    public static void registerAtlas(String name, Supplier<Atlas> supplier) {
        if (BrainGlobeAtlasId.hasVersion(name)) {
            throw new IllegalArgumentException("Atlas name '" + name + "' must not contain '"
                    + BrainGlobeAtlasId.SEPARATOR + "', which is reserved for BrainGlobe atlas versions");
        }
        if (extraAtlases.containsKey(name)) {
            System.err.println("Conflict : an atlas named "+name+" already exists. It will be overriden by the new one");
        }
        extraAtlases.put(name, supplier);
    }

    /**
     * Fetches the BrainGlobe catalogue, so that atlases not yet installed can be
     * offered for download. Called once; if the environment build fails it gives up
     * silently and does not retry this session — the installed atlases listed by
     * {@link BrainGlobeLocalInventory} do not depend on it.
     */
    private static synchronized void registerBrainGlobeAtlases(Context ctx) {
        if (brainGlobeRegistered) return;
        brainGlobeRegistered = true;

        AtlasLocationHelper.setContext(ctx);

        List<BrainGlobeAtlasId> bgAtlases = BrainGlobeAppose.getAvailableAtlasIds();
        if (!bgAtlases.isEmpty()) {
            System.out.println("Found " + bgAtlases.size() + " BrainGlobe atlases");
        }
    }

    /**
     * Maps each dropdown label back to the atlas name it stands for.
     * <p>
     * Labels are decorated ({@value #DOWNLOAD_PREFIX}, {@value #DOWNLOAD_SUFFIX}) and
     * the decoration is never parsed back off: it is looked up here instead, so that
     * changing how an entry reads can never change which atlas it opens. A null value
     * marks a row that is not an atlas at all, such as the separator.
     */
    private final Map<String, String> labelToAtlasName = new LinkedHashMap<>();

    protected void init() {

        String iniValue = this.choice;
        AtlasLocationHelper.setContext(ctx);

        labelToAtlasName.clear();
        final ArrayList<String> choices = new ArrayList<>();

        // Built-in Java atlases first: they are always present and need no download
        for (String builtIn : new String[]{
                AllenBrainAdultMouseAtlasCCF2017v3p1ASRCommand.atlasName,
                WaxholmSpragueDawleyRatV4p2ASRCommand.atlasName}) {
            choices.add(builtIn);
            labelToAtlasName.put(builtIn, builtIn);
        }

        // Then anything registered programmatically (custom atlases, scripts)
        final ArrayList<String> registered = new ArrayList<>(extraAtlases.keySet());
        Collections.sort(registered);
        for (String name : registered) {
            choices.add(name);
            labelToAtlasName.put(name, name);
        }

        // Then BrainGlobe atlases already materialized on disk. This is a fresh
        // filesystem scan on every init(), never a cache — which is what makes an
        // atlas downloaded a moment ago appear here next time with nothing to
        // invalidate — and it works with no network and no Python environment.
        final Set<String> installed = new LinkedHashSet<>();
        for (BrainGlobeAtlasId id : BrainGlobeLocalInventory.listMaterialized()) {
            String label = id.toString();
            installed.add(label);
            choices.add(label);
            labelToAtlasName.put(label, label);
        }

        // Finally the rest of the catalogue, marked as downloads. Only known once the
        // Python environment has been built, hence the placeholder until then.
        if (brainGlobeRegistered) {
            List<String> downloadable = new ArrayList<>();
            for (BrainGlobeAtlasId id : BrainGlobeAppose.getAvailableAtlasIds()) {
                if (installed.contains(id.toString())) continue;
                downloadable.add(id.toString());
            }
            if (!downloadable.isEmpty()) {
                Collections.sort(downloadable);
                choices.add(DOWNLOAD_SEPARATOR);
                labelToAtlasName.put(DOWNLOAD_SEPARATOR, null);
                for (String name : downloadable) {
                    String label = DOWNLOAD_PREFIX + name + DOWNLOAD_SUFFIX;
                    choices.add(label);
                    labelToAtlasName.put(label, name);
                }
            }
        } else {
            choices.add(BRAINGLOBE_OPTION);
            labelToAtlasName.put(BRAINGLOBE_OPTION, null);
        }

        final MutableModuleItem<String> input = getInfo().getMutableInput("choice",
                String.class);
        input.setChoices(choices);
        input.setValue(this, iniValue);
    }

    /**
     * Turns a dropdown label into an atlas name.
     * <p>
     * Falls back to the raw string so that a name typed by a script, which never
     * went through {@link #init()}, still resolves.
     *
     * @return the atlas name, or null if the row does not name an atlas
     */
    private String toAtlasName(String label) {
        if (label == null) return null;
        if (labelToAtlasName.containsKey(label)) {
            return labelToAtlasName.get(label);
        }
        return label;
    }

    final static String BRAINGLOBE_OPTION = "Get BrainGlobe Atlases...";
    final static String DOWNLOAD_SEPARATOR = "──────── not downloaded yet ────────";
    final static String DOWNLOAD_PREFIX = "⬇ ";
    final static String DOWNLOAD_SUFFIX = "   (download)";

    void checkBrainGlobe() {
        if (this.choice.equals(BRAINGLOBE_OPTION)) {
            if (!brainGlobeRegistered) {
                UIService uiService = ctx.getService(UIService.class);
                boolean headless = (uiService == null) || uiService.isHeadless();

                JDialog waitDialog;
                if (!headless) {
                    waitDialog = new JDialog((java.awt.Frame) null, "BrainGlobe Atlas List", true);
                    JPanel panel = new JPanel();
                    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
                    panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

                    JLabel infoLabel = new JLabel("<html>"
                            + "<h2>Fetching the BrainGlobe atlas list (v"+BrainGlobeAppose.BG_VERSION+")</h2>"
                            + "<p><img src='" + AtlasChooserCommand.class.getClassLoader().getResource("graphics/brainglobe.png") + "' width='80' height='80'></p>"
                            + "<p>For more information, visit <a href='https://brainglobe.info'>https://brainglobe.info</a></p>"
                            + "</html>", SwingConstants.CENTER);
                    infoLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
                    panel.add(infoLabel);

                    panel.add(Box.createVerticalStrut(15));

                    ImageIcon loadingIcon = new ImageIcon(AtlasChooserCommand.class.getClassLoader().getResource("graphics/loading.gif"));
                    loadingIcon.setImage(loadingIcon.getImage().getScaledInstance(64, 64, java.awt.Image.SCALE_DEFAULT));
                    JLabel waitLabel = new JLabel("<html><center>"
                            + "Building the Python environment and fetching the atlas list...<br>"
                            + "This is only needed to list atlases that are not downloaded yet."
                            + "</center></html>", loadingIcon, SwingConstants.CENTER);
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
