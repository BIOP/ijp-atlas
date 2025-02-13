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
import ch.epfl.biop.atlas.brainglobe.BrainGlobeHelper;
import ch.epfl.biop.atlas.struct.AtlasNode;

public class TestDeSerializeBrainGlobeOntology {
    public static void main(String... args) throws Exception {
        AtlasNode root = BrainGlobeHelper.buildTreeAndGetRoot("src/test/resources/structures_brainglobe_example.json");
        System.out.println(root.getId()); // Should be 997
        System.out.println(root.children());
    }

}
