package com.temenos.useragent.generic.mediatype;

/*
 * #%L
 * useragent-generic-java
 * %%
 * Copyright (C) 2012 - 2016 Temenos Holdings N.V.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import static com.temenos.useragent.generic.mediatype.AtomUtil.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.ParseException;
import org.apache.commons.io.IOUtils;

import com.temenos.useragent.generic.Link;
import com.temenos.useragent.generic.internal.EntityHandler;
import com.temenos.useragent.generic.internal.LinkImpl;

/**
 * An {@link EntityHandler entity handler} implementation for Atom Entry type in
 * <i>application/atom+xml</i> media type.
 * <p>
 * 
 * @see <a
 *      href="https://tools.ietf.org/html/rfc4287#section-4.1.2">atom:entry</a>
 *      element.
 *      </p>
 * @author ssethupathi
 *
 */
public class AtomEntryHandler implements EntityHandler {

	private Entry entry;

	public AtomEntryHandler() {
		entry = new Abdera().newEntry();
	}

	public List<Link> getLinks() {
		return convertLinks(entry.getLinks());
	}

	public String getId() {
		String fullPath = entry.getId().getPath();
		if (fullPath.contains("('") && fullPath.endsWith("')")) {
			return fullPath.substring(fullPath.indexOf("'") + 1,
					fullPath.lastIndexOf("'"));
		}
		return "";
	}

	public int getCount(String fqPropertyName) {
		String[] pathParts = validateAndParsePropertyName(fqPropertyName);
		Element parent = getParent(pathParts);
		String propertyName = pathParts[pathParts.length - 1];
		if (parent == null) {
			return 0;
		}
		Element propertyElement = parent.getFirstChild(new QName(NS_ODATA,
				buildElementName(propertyName)));
		return countSiblings(propertyElement);
	}

	private int countSiblings(Element propertyElement) {
		if (propertyElement == null) {
			return 0;
		}
		int count = 0;
		Element elementChild = propertyElement.getFirstChild(new QName(
				NS_ODATA, "element"));
		if (elementChild == null) {
			return 1;
		}
		while (elementChild != null) {
			count++;
			elementChild = elementChild.getNextSibling(new QName(NS_ODATA,
					"element"));
		}
		return count;
	}

	public String getValue(String fqPropertyName) {
		Element property = getProperty(fqPropertyName);
		if (property != null) {
			if (property.getFirstChild() == null) {
				return property.getText();
			} else {
				return getContent(property);
			}
		} else {
			return "";
		}
	}

	@Override
	public void setValue(String fqPropertyName, String value) {
		Element property = getProperty(fqPropertyName);
		if (property != null) {
			property.setText(value);
		} else {
			throw new RuntimeException("New value addition not supported");
		}
	}

	private Element getProperty(String fqPropertyName) {
		String[] pathParts = validateAndParsePropertyName(fqPropertyName);
		Element parent = getParent(pathParts);
		String propertyName = pathParts[pathParts.length - 1];
		if (parent != null) {
			return parent.getFirstChild(new QName(NS_ODATA, propertyName));
		} else {
			return null;
		}
	}

	private Element getParent(String... pathParts) {
		Element content = entry.getFirstChild(new QName(NS_ATOM, "content"));
		Element parent = content.getFirstChild(new QName(NS_ODATA_METADATA,
				"properties"));
		int pathIndex = 0;
		while (pathIndex < (pathParts.length - 1)) {
			String pathPart = pathParts[pathIndex];
			parent = getSpecificChild(parent, buildElementName(pathPart), 0);
			parent = getSpecificChild(parent, "element", extractIndex(pathPart));
			pathIndex++;
		}
		return parent;
	}

	private int extractIndex(String path) {
		if (path.matches(REGX_VALID_PART_WITH_INDEX)) {
			String indexStr = path.substring(path.indexOf("(") + 1,
					path.indexOf(")"));
			return Integer.parseInt(indexStr);
		}
		return 0;
	}

	private String buildElementName(String path) {
		if (path.matches(REGX_VALID_PART_WITH_INDEX)) {
			return path.substring(0, path.indexOf("("));
		}
		return path;
	}

	private String[] validateAndParsePropertyName(String fqName) {
		if (fqName == null || fqName.isEmpty()) {
			throw new IllegalArgumentException(
					"Invalid fully qualified property name '" + fqName);
		}
		String[] pathParts = fqName.split("/");
		int lastPartIndex = pathParts.length - 1;
		for (int index = 0; index < lastPartIndex; index++) {
			String pathPart = pathParts[index];
			if (!pathPart.matches(REGX_VALID_PART_WITH_INDEX)) {
				throw new IllegalArgumentException("Invalid part '" + pathPart
						+ "' in fully qualified property name '" + fqName + "'");
			}
		}
		String elementPart = pathParts[lastPartIndex];
		if (!elementPart.matches(REGX_VALID_ELEMENT)) {
			throw new IllegalArgumentException("Invalid property name '"
					+ elementPart + "'");
		}
		return pathParts;
	}

	private Element getSpecificChild(Element parent, String childName,
			int expectedIndex) {
		if (parent == null) {
			return null;
		}
		Element child = parent.getFirstChild(new QName(NS_ODATA, childName));
		if (expectedIndex == 0) {
			return child;
		} else {
			int index = 1;
			while (child != null) {
				child = child.getNextSibling(new QName(NS_ODATA, childName));
				if (expectedIndex == index++) {
					return child;
				}
			}
		}
		return null;
	}

	private List<Link> convertLinks(
			List<org.apache.abdera.model.Link> abderaLinks) {
		List<Link> links = new ArrayList<Link>();
		for (org.apache.abdera.model.Link abderaLink : abderaLinks) {
			AtomLinkHandler linkHandler = new AtomLinkHandler(abderaLink);
			links.add(new LinkImpl.Builder(abderaLink.getAttributeValue("href"))
					.baseUrl(linkHandler.getBaseUri())
					.rel(linkHandler.getRel())
					.id(getId())
					.title(abderaLink.getAttributeValue("title"))
					.description(
							AtomUtil.extractDescription(abderaLink
									.getAttributeValue("rel")))
					.payload(linkHandler.getEmbeddedPayload()).build());
		}
		return links;
	}

	@Override
	public void setContent(InputStream stream) {
		if (stream == null) {
			throw new IllegalArgumentException("Entity input stream is null");
		}
		Document<Element> entityDoc = null;
		try {
			entityDoc = new Abdera().getParser().parse(stream);
		} catch (ParseException e) {
			throw new IllegalArgumentException(
					"Unexpected entity for media type '" + AtomUtil.MEDIA_TYPE
							+ "'.", e);
		}
		QName rootElementQName = entityDoc.getRoot().getQName();
		if (new QName(AtomUtil.NS_ATOM, "entry").equals(rootElementQName)) {
			entry = (Entry) entityDoc.getRoot();
		} else {
			throw new IllegalArgumentException(
					"Unexpected entity for media type '" + MEDIA_TYPE
							+ "'. Payload [" + entityDoc.getRoot().toString()
							+ "]");
		}
	}

	public void setEntry(Entry entry) {
		this.entry = entry;
	}

	@Override
	public InputStream getContent() {
		return IOUtils.toInputStream(getContent(entry));
	}

	private String getContent(Element element) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			element.writeTo(baos);
			return baos.toString("UTF-8");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}