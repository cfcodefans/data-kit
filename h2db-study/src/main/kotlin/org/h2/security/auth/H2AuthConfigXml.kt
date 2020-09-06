package org.h2.security.auth

import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

/**
 * Parser of external authentication XML configuration file
 */
class H2AuthConfigXml : DefaultHandler() {
    companion object {
        @JvmStatic
        private fun getAttributeValueOr(attributeName: String, attributes: Attributes, defaultValue: String): String {
            val attributeValue: String? = attributes.getValue(attributeName)
            return if (attributeValue.isNullOrBlank()) defaultValue else attributeValue
        }

        @JvmStatic
        @Throws(SAXException::class)
        private fun getMandatoryAttributeValue(attributeName: String, attributes: Attributes): String {
            val attributeValue: String? = attributes.getValue(attributeName)
            if (attributeValue.isNullOrBlank())
                throw SAXException("missing attribute $attributeName")
            return attributeValue
        }

        /**
         * Parse the xml.
         *
         * @param url the source of the xml configuration.
         * @return Authenticator configuration.
         * @throws ParserConfigurationException if a parser connot be created.
         * @throws SAXException for SAX errors.
         * @throws IOException If an I/O error occurs
         */
        @JvmStatic
        @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
        fun parseFrom(url: URL): H2AuthConfig {
            return url.openStream().use { parseFrom(it) }
        }

        /**
         * Parse the xml.
         *
         * @param inputStream the source of the xml configuration.
         * @return Authenticator configuration.
         * @throws ParserConfigurationException if a parser cannot be created.
         * @throws SAXException for SAX errors.
         * @throws IOException If an I/O error occurs
         */
        @JvmStatic
        @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
        fun parseFrom(inputStream: InputStream): H2AuthConfig {
            val saxParser: SAXParser = SAXParserFactory.newInstance().newSAXParser()
            val xmlHandler: H2AuthConfigXml = H2AuthConfigXml()
            saxParser.parse(inputStream, xmlHandler)
            return xmlHandler.result!!
        }
    }

    var result: H2AuthConfig? = null
    private var lastConfigProperties: (() -> MutableList<PropertyConfig>)? = null

    @Throws(SAXException::class)
    override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes) {
        when (qName) {
            "h2Auth" -> {
                result = H2AuthConfig()
                result!!.allowUserRegistration = "true" == getAttributeValueOr("allowUserRegistration", attributes, "false")
            }
            "realm" -> {
                lastConfigProperties = RealmConfig().let {
                    it.name = getMandatoryAttributeValue("name", attributes)
                    it.validatorClass = getMandatoryAttributeValue("validatorClass", attributes)
                    it
                }
            }
            "userToRolesMapper" -> {
                lastConfigProperties = UserToRolesMapperConfig().let {
                    it.className = getMandatoryAttributeValue("className", attributes)
                    result!!.userToRolesMappers!! += it
                    it
                }
            }
            "property" -> {
                if (lastConfigProperties == null) {
                    throw SAXException("property element in the wrong place")
                }
                lastConfigProperties!!().add(PropertyConfig(
                        name = getMandatoryAttributeValue("name", attributes),
                        value = getMandatoryAttributeValue("value", attributes)
                ))
            }
            else -> throw SAXException("unexpected element $qName")
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (lastConfigProperties != null && qName != "property") {
            lastConfigProperties = null
        }
    }
}