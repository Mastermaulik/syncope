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
package org.apache.syncope.console.commons;

import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.console.pages.AbstractSchemaModalPage;
import org.apache.syncope.console.pages.BaseModalPage;
import org.apache.syncope.console.pages.DerivedSchemaModalPage;
import org.apache.syncope.console.pages.SchemaModalPage;
import org.apache.syncope.console.pages.VirtualSchemaModalPage;
import org.apache.syncope.console.rest.SchemaRestClient;
import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Modal window with Schema form.
 */
abstract public class SchemaModalPageFactory extends BaseModalPage {

    private static final long serialVersionUID = -3533177688264693505L;

    @SpringBean
    protected SchemaRestClient restClient;

    public enum SchemaType {

        NORMAL,
        DERIVED,
        VIRTUAL

    };

    public static AbstractSchemaModalPage getSchemaModalPage(AttributableType entity, SchemaType schemaType) {

        AbstractSchemaModalPage page;

        switch (schemaType) {
            case DERIVED:
                page = new DerivedSchemaModalPage(entity);
                break;
            case VIRTUAL:
                page = new VirtualSchemaModalPage(entity);
                break;
            default:
                page = new SchemaModalPage(entity);
                break;
        }

        return page;
    }
}
