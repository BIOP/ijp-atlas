package ch.epfl.biop.atlas.rat.waxholm.spraguedawley.ilfparser;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD) // Not sure what this does
public class Label {

    @XmlAttribute
    public String abbreviation;

    @XmlAttribute
    public String color;

    @XmlAttribute
    public String id;

    @XmlAttribute
    public String name;

    @XmlElement(name="label")
    public Label[] labels;
}
