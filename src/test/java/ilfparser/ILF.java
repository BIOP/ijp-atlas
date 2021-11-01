package ilfparser;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "milf")
public class ILF {
    @XmlElement(name="meta")
    public Meta meta;

    @XmlElement(name="structure")
    public Structure structure;
}
