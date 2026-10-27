package com.banamex.cops.update_offer.testutil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

public final class XmlTestUtil {

    private XmlTestUtil() { }

    /** Parsea el XML; si está mal formado la prueba falla con el XML en el mensaje. */
    public static Document parse(String xml) {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            return f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new AssertionError("XML mal formado: " + e.getMessage() + "\n" + xml, e);
        }
    }

    /** Nombres de los hijos directos del primer elemento con ese nombre. */
    public static List<String> childNames(Document doc, String parentName) {
        NodeList parents = doc.getElementsByTagName(parentName);
        List<String> names = new ArrayList<>();
        if (parents.getLength() == 0) return names;
        NodeList children = parents.item(0).getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                names.add(children.item(i).getNodeName());
            }
        }
        return names;
    }

    /** Texto del primer elemento con ese nombre (null si no existe). */
    public static String text(Document doc, String name) {
        NodeList l = doc.getElementsByTagName(name);
        return l.getLength() == 0 ? null : l.item(0).getTextContent();
    }

    public static boolean hasUrls(String xml) {
        return xml.contains("xmlns") || xml.contains("http://") || xml.contains("https://");
    }
}
