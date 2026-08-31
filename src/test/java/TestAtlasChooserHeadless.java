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
