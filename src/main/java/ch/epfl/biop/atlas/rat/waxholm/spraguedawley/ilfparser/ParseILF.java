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
package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.ilfparser;

import javax.xml.bind.JAXBContext;
import java.io.FileReader;

public class ParseILF {

    public static void main(String... args) throws Exception {

        // Un marshall xml

        String path = "src/test/resources/";

        JAXBContext context = JAXBContext.newInstance(ILF.class);
        ILF ilf = (ILF) context.createUnmarshaller()
                .unmarshal(new FileReader(path+"WHS_SD_rat_atlas_v4_labels.ilf"));

        System.out.println(ilf.meta.filename);
        System.out.println(ilf.structure.labels.length);
        System.out.println(ilf.structure.labels[2].labels[0].abbreviation);
    }
}
