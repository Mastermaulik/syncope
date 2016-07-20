/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.client.lib;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.syncope.common.lib.jackson.UnwrappedObjectMapper;
import org.apache.syncope.common.lib.jackson.UnwrappedXmlMapper;
import org.apache.syncope.common.rest.api.DateParamConverterProvider;
import org.apache.syncope.common.rest.api.RESTHeaders;

/**
 * Factory bean for creating instances of {@link SyncopeClient}.
 * Supports Spring-bean configuration and override via subclassing (see protected methods).
 */
public class SyncopeClientFactoryBean {

    public enum ContentType {

        JSON(MediaType.APPLICATION_JSON_TYPE),
        XML(MediaType.APPLICATION_XML_TYPE);

        private final MediaType mediaType;

        ContentType(final MediaType mediaType) {
            this.mediaType = mediaType;
        }

        public MediaType getMediaType() {
            return mediaType;
        }

        public static ContentType fromString(final String value) {
            return StringUtils.isNotBlank(value) && value.equalsIgnoreCase(XML.getMediaType().toString())
                    ? XML
                    : JSON;
        }
    }

    private JacksonJsonProvider jsonProvider;

    private JacksonXMLProvider xmlProvider;

    private RestClientExceptionMapper exceptionMapper;

    private String address;

    private ContentType contentType;

    private String domain;

    private boolean useCompression;

    private RestClientFactoryBean restClientFactoryBean;

    protected JacksonJsonProvider defaultJsonProvider() {
        return new JacksonJsonProvider(new UnwrappedObjectMapper(), JacksonJsonProvider.BASIC_ANNOTATIONS);
    }

    protected JacksonXMLProvider defaultXMLProvider() {
        return new JacksonXMLProvider(new UnwrappedXmlMapper(), JacksonXMLProvider.BASIC_ANNOTATIONS);
    }

    protected RestClientExceptionMapper defaultExceptionMapper() {
        return new RestClientExceptionMapper();
    }

    protected RestClientFactoryBean defaultRestClientFactoryBean() {
        RestClientFactoryBean defaultRestClientFactoryBean = new RestClientFactoryBean();

        if (StringUtils.isBlank(address)) {
            throw new IllegalArgumentException("Property 'address' is missing");
        }
        defaultRestClientFactoryBean.setAddress(address);

        if (StringUtils.isNotBlank(domain)) {
            defaultRestClientFactoryBean.setHeaders(Collections.singletonMap(RESTHeaders.DOMAIN, domain));
        }

        defaultRestClientFactoryBean.setThreadSafe(true);
        defaultRestClientFactoryBean.setInheritHeaders(true);

        List<Feature> features = new ArrayList<>();
        features.add(new LoggingFeature());
        defaultRestClientFactoryBean.setFeatures(features);

        List<Object> providers = new ArrayList<>(4);
        providers.add(new DateParamConverterProvider());
        providers.add(getXMLProvider());
        providers.add(getJsonProvider());
        providers.add(getExceptionMapper());
        defaultRestClientFactoryBean.setProviders(providers);

        return defaultRestClientFactoryBean;
    }

    public JacksonJsonProvider getJsonProvider() {
        return jsonProvider == null
                ? defaultJsonProvider()
                : jsonProvider;
    }

    public void setJsonProvider(final JacksonJsonProvider jsonProvider) {
        this.jsonProvider = jsonProvider;
    }

    public JacksonXMLProvider getXMLProvider() {
        return xmlProvider == null
                ? defaultXMLProvider()
                : xmlProvider;
    }

    public SyncopeClientFactoryBean setXMLProvider(final JacksonXMLProvider xmlProvider) {
        this.xmlProvider = xmlProvider;
        return this;
    }

    public RestClientExceptionMapper getExceptionMapper() {
        return exceptionMapper == null
                ? defaultExceptionMapper()
                : exceptionMapper;
    }

    public SyncopeClientFactoryBean setExceptionMapper(final RestClientExceptionMapper exceptionMapper) {
        this.exceptionMapper = exceptionMapper;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public SyncopeClientFactoryBean setAddress(final String address) {
        this.address = address;
        return this;
    }

    public ContentType getContentType() {
        return contentType == null
                ? ContentType.JSON
                : contentType;
    }

    public SyncopeClientFactoryBean setContentType(final ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    public SyncopeClientFactoryBean setContentType(final String contentType) {
        this.contentType = ContentType.fromString(contentType);
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public SyncopeClientFactoryBean setDomain(final String domain) {
        this.domain = domain;
        return this;
    }

    /**
     * Sets the given service instance for transparent gzip <tt>Content-Encoding</tt> handling.
     *
     * @param useCompression whether transparent gzip <tt>Content-Encoding</tt> handling is to be enabled
     * @return the current instance
     */
    public SyncopeClientFactoryBean setUseCompression(final boolean useCompression) {
        this.useCompression = useCompression;
        return this;
    }

    public boolean isUseCompression() {
        return useCompression;
    }

    public RestClientFactoryBean getRestClientFactoryBean() {
        return restClientFactoryBean == null
                ? defaultRestClientFactoryBean()
                : restClientFactoryBean;
    }

    public SyncopeClientFactoryBean setRestClientFactoryBean(final RestClientFactoryBean restClientFactoryBean) {
        this.restClientFactoryBean = restClientFactoryBean;
        return this;
    }

    /**
     * Builds client instance with no authentication, for user self-registration and related queries (schema,
     * resources, ...).
     *
     * @return client instance with no authentication
     */
    public SyncopeClient create() {
        return create(null, null);
    }

    /**
     * Builds client instance with the given credentials.
     *
     * @param username username
     * @param password password
     * @return client instance with the given credentials
     */
    public SyncopeClient create(final String username, final String password) {
        return new SyncopeClient(
                getContentType().getMediaType(),
                getRestClientFactoryBean(),
                getExceptionMapper(),
                username,
                password,
                useCompression);
    }
}
