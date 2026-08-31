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
