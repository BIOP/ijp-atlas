package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.ilfparser;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD) // Not sure what this does
public class Structure {
    @XmlElement(name="label")
    public Label[] labels;
}
