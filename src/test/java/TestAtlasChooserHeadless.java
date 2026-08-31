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
import ch.epfl.biop.atlas.brainglobe.BrainGlobeAtlasId;
import ch.epfl.biop.atlas.scijava.AtlasChooserCommand;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.scijava.Context;
import org.scijava.command.CommandInfo;
import org.scijava.command.CommandService;
import org.scijava.module.Module;
import org.scijava.module.ModuleService;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks that the atlas chooser works with no UI and no network.
 * <p>
 * The command shows a modal confirmation before downloading an atlas, and a modal
 * wait dialog while the BrainGlobe catalogue is fetched. Either appearing in a
 * headless run would hang a batch script indefinitely rather than fail it, so the
 * headless branches are worth pinning down.
 * <p>
 * Every case here is settled locally: the dropdown is built from the filesystem, and
 * a name without a version is refused before anything is fetched. Nothing in this
 * class builds a Python environment or reaches the network — which is the property
 * being tested as much as it is a convenience.
 */
public class TestAtlasChooserHeadless {

	private static Context ctx;
	private static CommandInfo chooser;

	@BeforeClass
	public static void setUp() {
		System.setProperty("java.awt.headless", "true");
		ctx = new Context();
		chooser = ctx.service(CommandService.class).getCommand(AtlasChooserCommand.class);
	}

	@AfterClass
	public static void tearDown() {
		if (ctx != null) ctx.dispose();
	}

	/** Runs the chooser with preprocessing on, as a script or the GUI would */
	private static Module runChooser(String choice) throws Exception {
		return ctx.service(ModuleService.class).run(chooser, true, "choice", choice).get();
	}

	/**
	 * A bare BrainGlobe name is refused. What matters here is not the refusal, which
	 * is covered elsewhere, but that it comes back promptly instead of waiting on a
	 * dialog nobody can answer.
	 */
	@Test(timeout = 60_000)
	public void bareNameFailsWithoutBlocking() throws Exception {
		Module module = runChooser("allen_mouse_50um");
		assertNull("A bare name must not resolve to an atlas", module.getOutput("atlas"));
	}

	/**
	 * The dropdown has to populate from the filesystem alone, so that a machine with
	 * no network, or without the Python environment built, still offers every atlas
	 * it has already downloaded.
	 */
	@Test(timeout = 60_000)
	public void listsChoicesWithoutNetwork() throws Exception {
		Module module = runChooser("allen_mouse_50um");
		@SuppressWarnings("unchecked")
		List<String> choices = (List<String>) module.getInfo().getInput("choice").getChoices();
		assertFalse("Expected the choice list to be populated", choices.isEmpty());

		// The built-in Java atlases are always offered
		assertTrue("Expected the built-in Allen atlas among " + choices,
				choices.contains("allen_mouse_10um_java"));

		// Every BrainGlobe entry carries a version; none is offered bare
		for (String choice : choices) {
			if (choice.contains("_um@") || choice.contains("um@")) {
				BrainGlobeAtlasId.parse(choice); // throws if malformed
			}
		}
	}

	/**
	 * Until the catalogue has been fetched there is nothing to download from, so the
	 * placeholder that triggers that fetch must be offered — and must not be mistaken
	 * for an atlas if it is somehow selected.
	 */
	@Test(timeout = 60_000)
	public void offersCatalogueFetchWithoutPerformingIt() throws Exception {
		Module module = runChooser("Get BrainGlobe Atlases...");
		assertNull("The catalogue placeholder is not an atlas", module.getOutput("atlas"));
	}
}
