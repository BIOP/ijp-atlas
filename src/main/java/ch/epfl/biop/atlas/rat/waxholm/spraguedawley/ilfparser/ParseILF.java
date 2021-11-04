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
