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
package org.apache.syncope.client.console.wicket.markup.html.list;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggle;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.checkbox.bootstraptoggle.BootstrapToggleConfig;
import java.io.Serializable;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.client.console.commons.Constants;
import org.apache.syncope.client.console.wicket.markup.html.form.AbstractFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxCheckBoxPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxPasswordFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.AjaxTextFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.FieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.MultiFieldPanel;
import org.apache.syncope.client.console.wicket.markup.html.form.SpinnerFieldPanel;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public class ConnConfPropertyListView extends ListView<ConnConfProperty> {

    private static final long serialVersionUID = -5239334900329150316L;

    private static final Logger LOG = LoggerFactory.getLogger(ConnConfPropertyListView.class);

    private final boolean withOverridable;

    public ConnConfPropertyListView(
            final String id,
            final IModel<? extends List<ConnConfProperty>> model,
            final boolean withOverridable) {

        super(id, model);
        this.withOverridable = withOverridable;
    }

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void populateItem(final ListItem<ConnConfProperty> item) {
        final ConnConfProperty property = item.getModelObject();

        final Label label = new Label("connPropAttrSchema", StringUtils.isBlank(property.getSchema().getDisplayName())
                ? property.getSchema().getName() : property.getSchema().getDisplayName());
        item.add(label);

        final FieldPanel<? extends Serializable> field;
        boolean required = false;
        boolean isArray = false;

        if (property.getSchema().isConfidential()
                || Constants.GUARDED_STRING.equalsIgnoreCase(property.getSchema().getType())
                || Constants.GUARDED_BYTE_ARRAY.equalsIgnoreCase(property.getSchema().getType())) {

            field = new AjaxPasswordFieldPanel(
                    "panel", label.getDefaultModelObjectAsString(), new Model<String>(), false);
            ((PasswordTextField) field.getField()).setResetPassword(false);

            required = property.getSchema().isRequired();
        } else {
            Class<?> propertySchemaClass;
            try {
                propertySchemaClass = ClassUtils.forName(property.getSchema().getType(), ClassUtils.
                        getDefaultClassLoader());
                if (ClassUtils.isPrimitiveOrWrapper(propertySchemaClass)) {
                    propertySchemaClass = org.apache.commons.lang3.ClassUtils.primitiveToWrapper(propertySchemaClass);
                }
            } catch (ClassNotFoundException e) {
                LOG.error("Error parsing attribute type", e);
                propertySchemaClass = String.class;
            }

            if (ClassUtils.isAssignable(Number.class, propertySchemaClass)) {
                @SuppressWarnings("unchecked")
                final Class<Number> numberClass = (Class<Number>) propertySchemaClass;
                field = new SpinnerFieldPanel<Number>(
                        "panel", label.getDefaultModelObjectAsString(), numberClass, new Model<Number>());

                required = property.getSchema().isRequired();
            } else if (ClassUtils.isAssignable(Boolean.class, propertySchemaClass)) {
                field = new AjaxCheckBoxPanel(
                        "panel", label.getDefaultModelObjectAsString(), new Model<Boolean>(), false);
            } else {
                field = new AjaxTextFieldPanel(
                        "panel", label.getDefaultModelObjectAsString(), new Model<String>(), false);
                required = property.getSchema().isRequired();
            }

            if (propertySchemaClass.isArray()) {
                isArray = true;
            }
        }

        field.setTitle(property.getSchema().getHelpMessage());

        final AbstractFieldPanel<? extends Serializable> fieldPanel;
        if (isArray) {
            final MultiFieldPanel multiFieldPanel = new MultiFieldPanel(
                    "panel",
                    label.getDefaultModelObjectAsString(),
                    new PropertyModel<List<String>>(property, "values"),
                    field, true);
            item.add(multiFieldPanel);
            fieldPanel = multiFieldPanel;
        } else {
            setNewFieldModel(field, property.getValues());
            item.add(field);
            fieldPanel = field;
        }

        if (required) {
            fieldPanel.addRequiredLabel();
        }

        if (withOverridable) {
            fieldPanel.showExternAction(addCheckboxToggle(property));
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void setNewFieldModel(final FieldPanel field, final List<Object> values) {
        field.setNewModel(values);
    }

    private FormComponent<?> addCheckboxToggle(final ConnConfProperty property) {

        final BootstrapToggleConfig config = new BootstrapToggleConfig();
        config
                .withOnStyle(BootstrapToggleConfig.Style.info).withOffStyle(BootstrapToggleConfig.Style.warning)
                .withSize(BootstrapToggleConfig.Size.mini)
                .withOnLabel("Overridable")
                .withOffLabel("Not Overridable");

        return new BootstrapToggle("externalAction", new PropertyModel<Boolean>(property, "overridable"), config) {

            private static final long serialVersionUID = 1L;

            @Override
            protected CheckBox newCheckBox(final String id, final IModel<Boolean> model) {
                final CheckBox checkBox = super.newCheckBox(id, model);
                checkBox.add(new AjaxFormComponentUpdatingBehavior("change") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(final AjaxRequestTarget target) {
                    }
                });
                return checkBox;
            }

            @Override
            protected void onComponentTag(final ComponentTag tag) {
                super.onComponentTag(tag);
                tag.append("class", "overridable", " ");
            }
        };
    }
}
