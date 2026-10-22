package com.banamex.cops.update_offer.mapper;

/** Utilidades para escribir XML a mano (equivale al render-xml / serialización de BW). */
final class XmlUtil {

    private XmlUtil() { }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    /** &lt;name&gt;value&lt;/name&gt;. Omite el elemento si value es null. */
    static void tag(StringBuilder x, String indent, String name, String value) {
        if (value == null) return;
        x.append(indent).append('<').append(name).append('>').append(esc(value))
                .append("</").append(name).append(">\n");
    }

    /** Igual que tag, pero null o vacío se escribe como &lt;name/&gt; (como lo hace el log de BW). */
    static void tagOrEmpty(StringBuilder x, String indent, String name, String value) {
        if (value == null || value.isEmpty()) {
            x.append(indent).append('<').append(name).append("/>\n");
        } else {
            tag(x, indent, name, value);
        }
    }

    /** &lt;p:name xmlns:p="ns"&gt;value&lt;/p:name&gt; o &lt;p:name xmlns:p="ns"/&gt; si value es null o vacío. */
    static void nsTag(StringBuilder x, String indent, String prefix, String name, String ns, String value) {
        x.append(indent).append('<').append(prefix).append(':').append(name)
                .append(" xmlns:").append(prefix).append("=\"").append(ns).append('"');
        if (value == null || value.isEmpty()) {
            x.append("/>\n");
        } else {
            x.append('>').append(esc(value)).append("</").append(prefix).append(':').append(name).append(">\n");
        }
    }
}
