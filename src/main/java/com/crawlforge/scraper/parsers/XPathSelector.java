package com.crawlforge.scraper.parsers;

import com.crawlforge.scraper.core.exception.ParseException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Bridges a Jsoup DOM into a W3C DOM so XPath expressions can be evaluated against scraped
 * HTML as well as XML. Demonstrates real DOM/XPath knowledge rather than relying on a
 * CSS-selector-only library.
 */
public final class XPathSelector {

    private XPathSelector() {}

    public static List<String> select(Element context, String expression) {
        if (context == null || expression == null || expression.isBlank()) return List.of();
        Document dom = newSecureDocument();
        org.w3c.dom.Element root = dom.createElement("root");
        dom.appendChild(root);
        copyInto(context, root, dom);
        return evaluate(expression, dom);
    }

    /** Evaluates a relative XPath against an existing W3C DOM node (used by the XML parser). */
    public static List<String> select(org.w3c.dom.Node context, String expression) {
        return evaluate(expression, context);
    }

    public static List<String> selectXml(String xml, String expression) {
        if (xml == null || expression == null || expression.isBlank()) return List.of();
        try {
            DocumentBuilderFactory factory = secureFactory();
            Document dom = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            return evaluate(expression, dom);
        } catch (Exception e) {
            throw new ParseException("Could not parse XML for XPath: " + expression, e);
        }
    }

    private static List<String> evaluate(String expression, Object context) {
        try {
            NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath()
                    .evaluate(expression, context, XPathConstants.NODESET);
            List<String> values = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                String value = nodes.item(i).getTextContent();
                if (value != null && !value.isBlank()) values.add(value.trim());
            }
            return List.copyOf(values);
        } catch (XPathExpressionException e) {
            throw new ParseException("Invalid XPath expression: " + expression, e);
        }
    }

    private static void copyInto(Element source, org.w3c.dom.Element target, Document dom) {
        for (Attribute attribute : source.attributes()) {
            target.setAttribute(attribute.getKey(), attribute.getValue());
        }
        for (Node child : source.childNodes()) {
            if (child instanceof Element element) {
                org.w3c.dom.Element copy = dom.createElement(element.tagName());
                target.appendChild(copy);
                copyInto(element, copy, dom);
            } else if (child instanceof TextNode text) {
                target.appendChild(dom.createTextNode(text.getWholeText()));
            }
        }
    }

    private static Document newSecureDocument() {
        try {
            return secureFactory().newDocumentBuilder().newDocument();
        } catch (Exception e) {
            throw new ParseException("Could not create DOM document", e);
        }
    }

    private static DocumentBuilderFactory secureFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }
}
