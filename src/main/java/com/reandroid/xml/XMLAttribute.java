/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid.xml;

import com.reandroid.common.Namespace;
import com.reandroid.utils.ObjectsUtil;
import com.reandroid.xml.base.Attribute;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;

public class XMLAttribute extends XMLNode implements Attribute {
    
    private String mName;
    private String mValue;
    private XMLNamespace mNamespace;
    
    public XMLAttribute() {
        super();
    }
    public XMLAttribute(String name, String value) {
        this();
        mName = name;
        mValue = value;
    }
    @Override
    public XMLElement getParentNode() {
        return (XMLElement) super.getParentNode();
    }
    public boolean equalsName(String name) {
        if (name == null) {
            return getName() == null;
        }
        String prefix = XMLUtil.splitPrefix(name);
        if (prefix != null && !prefix.equals(getPrefix())) {
            return false;
        }
        return name.equals(getName());
    }
    public String getName() {
        return getName(false);
    }
    public String getName(boolean includePrefix) {
        String name = mName;
        if (!includePrefix || name == null) {
            return name;
        }
        String prefix = getPrefix();
        if (prefix != null) {
            name = prefix + ":" + name;
        }
        return name;
    }
    public XMLNamespace getNamespace() {
        return mNamespace;
    }
    public void setNamespace(Namespace namespace) {
        this.mNamespace = (XMLNamespace) namespace;
    }
    public void setNamespaceFrom(Namespace namespace) {
        String uri;
        String prefix;
        if (namespace != null) {
            uri = namespace.getUri();
            prefix = namespace.getPrefix();
        } else {
            uri = null;
            prefix = null;
        }
        setNamespace(uri, prefix);
    }
    public void setNamespace(String uri, String prefix) {
        XMLElement element = getParentNode();
        if (element == null) {
            throw new IllegalArgumentException("Parent element is null");
        }
        setNamespace(element.getOrCreateXMLNamespace(uri, prefix));
    }
    public String getPrefix() {
        XMLNamespace namespace = getNamespace();
        if (namespace != null) {
            return namespace.getPrefix();
        }
        String name = this.mName;
        int i = name.indexOf(':');
        if (i > 0) {
            return name.substring(0, i);
        }
        return null;
    }
    public String getValueAsString() {
        return getValueAsString(false);
    }
    public String getValueAsString(boolean escapeXmlText) {
        String value = this.mValue;
        if (value == null) {
            value = "";
            this.mValue = value;
        }
        if (escapeXmlText) {
            return XMLUtil.escapeXmlChars(value, true);
        }
        return value;
    }
    void setFrom(XMLAttribute xmlAttribute) {
        set(xmlAttribute.getName(true),
                xmlAttribute.getValueAsString(false));
        setNamespaceFrom(xmlAttribute.getNamespace());
    }
    XMLAttribute set(String name, String value) {
        this.mName = name;
        this.mValue = value;
        return this;
    }
    public void setName(String name) {
        setName(null, null, name);
    }
    public void setName(String uri, String name) {
        setName(uri, null, name);
    }
    public void setName(String uri, String prefix, String name) {
        mName = XMLUtil.splitName(name);
        if (XMLUtil.isEmpty(prefix)) {
            prefix = XMLUtil.splitPrefix(name);
        }
        if (XMLUtil.isEmpty(uri)) {
            uri = null;
        }
        XMLElement element = getParentNode();
        if (element == null) {
            throw new IllegalArgumentException("Parent element is null");
        }
        XMLNamespace namespace = null;
        if (uri != null && prefix != null) {
            namespace = element.getOrCreateXMLNamespace(uri, prefix);
        } else if (uri != null) {
            namespace = element.getXMLNamespaceByUri(uri);
            if (namespace == null) {
                throw new IllegalArgumentException("Namespace not found for uri: " + uri);
            }
        } else if (prefix != null) {
            namespace = element.getXMLNamespaceByPrefix(prefix);
            if (namespace == null) {
                throw new IllegalArgumentException("Namespace not found for prefix: " + prefix);
            }
        }
        if (namespace != null) {
            setNamespace(namespace);
        }
        mName = name;
    }
    public void setValue(String value) {
        mValue = value;
    }
    public void setPrefix(String prefix) {
        if (Objects.equals(prefix, getPrefix())) {
            return;
        }
        XMLElement element = getParentNode();
        if (element == null) {
            throw new IllegalArgumentException("Parent element is null");
        }
        setNamespace(element.getXMLNamespaceByPrefix(prefix));
    }

    @Override
    public void serialize(XmlSerializer serializer) throws IOException {
        serializer.attribute(getUri(), getName(), getValueAsString(false));
    }
    @Override
    void write(Appendable appendable, boolean xml, boolean escapeXmlText) throws IOException {
        appendable.append(getName(true));
        appendable.append('=');
        if (xml) {
            appendable.append('"');
        }
        appendable.append(getValueAsString(escapeXmlText));
        if (xml) {
            appendable.append('"');
        }
    }

    @Override
    public int hashCode() {
        return ObjectsUtil.hash(getName(false));
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof XMLAttribute) {
            XMLAttribute attribute = (XMLAttribute)obj;
            return ObjectsUtil.equals(getName(false), attribute.getName(false));
        }
        return false;
    }

    @Override
    public String toString() {
        return getName(true) + "=\"" + getValueAsString() + "\"";
    }
}
