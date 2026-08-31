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
import ch.epfl.biop.atlas.brainglobe.BrainGlobeLocalInventory;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests the versioned BrainGlobe atlas id and the local inventory.
 * <p>
 * Neither touches the network nor Python, which is the point: these are the parts
 * that have to keep working when BrainGlobe cannot be reached.
 */
public class TestBrainGlobeAtlasId {

	@Test
	public void parsesNameAndVersion() {
		BrainGlobeAtlasId id = BrainGlobeAtlasId.parse("allen_mouse_50um@3.1");
		assertEquals("allen_mouse_50um", id.getName());
		assertEquals("3.1", id.getVersion());
		assertEquals("allen_mouse_50um@3.1", id.toString());
	}

	/**
	 * 14 of the 222 published atlases have a dot in their name, so a parser that
	 * looked for the version by splitting on dots would mangle them.
	 */
	@Test
	public void keepsDotsInsideAtlasNames() {
		BrainGlobeAtlasId id = BrainGlobeAtlasId.parse("kim_dev_mouse_e15-5_mri-adc_37.5um@3.0");
		assertEquals("kim_dev_mouse_e15-5_mri-adc_37.5um", id.getName());
		assertEquals("3.0", id.getVersion());
	}

	/** A bare name must never be resolved to "whatever version happens to be latest" */
	@Test
	public void refusesBareName() {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
				() -> BrainGlobeAtlasId.parse("allen_mouse_50um"));
		// The message has to say how to fix it, since this is the error users will hit
		assertTrue(e.getMessage(), e.getMessage().contains("allen_mouse_50um@"));
	}

	@Test
	public void refusesNonNumericVersion() {
		assertThrows(IllegalArgumentException.class,
				() -> BrainGlobeAtlasId.parse("allen_mouse_50um@latest"));
		assertThrows(IllegalArgumentException.class,
				() -> BrainGlobeAtlasId.parse("allen_mouse_50um@"));
	}

	@Test
	public void detectsPresenceOfAVersion() {
		assertTrue(BrainGlobeAtlasId.hasVersion("allen_mouse_50um@3.1"));
		assertFalse(BrainGlobeAtlasId.hasVersion("allen_mouse_50um"));
		assertFalse(BrainGlobeAtlasId.hasVersion("Rat - Waxholm Sprague Dawley V4p2"));
		assertFalse(BrainGlobeAtlasId.hasVersion(null));
	}

	/** Versions are dotted in the id and underscored on disk */
	@Test
	public void mapsVersionToItsFolderName() {
		assertEquals("3_1", BrainGlobeAtlasId.parse("allen_mouse_50um@3.1").getVersionFolder());
		assertEquals("3", BrainGlobeAtlasId.parse("allen_mouse_50um@3").getVersionFolder());
	}

	@Test
	public void equalityIsByNameAndVersion() {
		assertEquals(BrainGlobeAtlasId.parse("a@1.0"), BrainGlobeAtlasId.of("a", "1.0"));
		assertEquals(BrainGlobeAtlasId.parse("a@1.0").hashCode(), BrainGlobeAtlasId.of("a", "1.0").hashCode());
		assertFalse(BrainGlobeAtlasId.parse("a@1.0").equals(BrainGlobeAtlasId.parse("a@2.0")));
	}

	/**
	 * The inventory must survive a machine with no BrainGlobe installation at all,
	 * returning nothing rather than failing — a listing failure must never be what
	 * hides the atlases someone already has.
	 */
	@Test
	public void inventoryNeverThrows() {
		assertNotNull(BrainGlobeLocalInventory.listMaterialized());
		assertNotNull(BrainGlobeLocalInventory.getBrainGlobeDir());
		assertFalse(BrainGlobeLocalInventory.isMaterialized(
				BrainGlobeAtlasId.of("no_such_atlas_at_all", "9.9")));
	}

	/** The versioned folder layout is what makes two versions installable side by side */
	@Test
	public void locatesTheVersionedAtlasFolder() {
		File dir = BrainGlobeLocalInventory.getAtlasDir(BrainGlobeAtlasId.of("allen_mouse_50um", "3.1"));
		assertEquals("3_1", dir.getName());
		assertEquals("allen_mouse_50um", dir.getParentFile().getName());
		assertEquals("atlases", dir.getParentFile().getParentFile().getName());
	}
}
