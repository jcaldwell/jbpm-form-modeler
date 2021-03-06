/**
 * Copyright (C) 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.formModeler.components.editor;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import org.jbpm.formModeler.core.config.DataHolderManager;
import org.jbpm.formModeler.core.config.FieldTypeManager;
import org.jbpm.formModeler.api.model.DataFieldHolder;
import org.jbpm.formModeler.api.model.DataHolder;
import org.jbpm.formModeler.api.model.FieldType;
import org.jbpm.formModeler.api.model.Form;
import org.jbpm.formModeler.core.config.builders.DataHolderBuilder;
import org.jbpm.formModeler.service.bb.mvc.taglib.formatter.Formatter;
import org.jbpm.formModeler.service.bb.mvc.taglib.formatter.FormatterException;
import org.slf4j.LoggerFactory;


import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@Named("DataHoldersFormFormatter")
public class DataHoldersFormFormatter extends Formatter {

    private Logger log = LoggerFactory.getLogger(DataHoldersFormFormatter.class);

    @Inject
    private DataHolderManager dataHolderManager;

    @Inject
    private FieldTypeManager fieldTypeManager;

    /**
     * The comparator implementation for form holder types.
     */
    private static final Comparator<String> formHolderTypesComparator = new Comparator<String>() {
        /**
         * Default implementation for comparing form holder types will be using <code>String#compareTo</code> method.
         *
         * @param o1 The object source.
         * @param o2 The obect to compare.
         * @return The compare result.
         */
        @Override
        public int compare(String o1, String o2) {
            return o1.compareTo(o2);
        }
    };

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws FormatterException {
        try {
            WysiwygFormEditor wysiwygFormEditor = WysiwygFormEditor.lookup();
            if (WysiwygFormEditor.EDITION_OPTION_BINDINGS_FIELDS.equals(wysiwygFormEditor.getCurrentEditionOption())) {
                renderPendingFields();
            } else {
                renderDataHolders();
            }
        } catch (Exception e) {
            log.error(" DataHoldersFormFormatter rendering error");
        }
    }

    public void renderDataHolders() {
        WysiwygFormEditor wysiwygFormEditor = WysiwygFormEditor.lookup();
        try {
            renderFragment("outputStart");

            Form form = wysiwygFormEditor.getCurrentForm();
            Set<DataHolder> holders = form.getHolders();
            String existingIds ="\"\"";
            String existingInputIds ="\"\"";
            String existingOutputIds ="\"\"";
            for (DataHolder holder : holders) {
                if (!StringUtils.isEmpty(holder.getInputId())) existingInputIds+= ", \""+holder.getInputId()+"\" ";
                if (!StringUtils.isEmpty(holder.getOuputId())) existingOutputIds+= ", \""+holder.getOuputId()+"\" ";
                if (!StringUtils.isEmpty(holder.getUniqeId())) existingIds+= ", \""+holder.getUniqeId()+"\" ";
            }

            setAttribute("existingIds", existingIds);
            setAttribute("existingInputIds", existingInputIds);
            setAttribute("existingOutputIds", existingOutputIds);
            renderFragment("outputFormAddHolderStart");

            Map<String, String> colors = dataHolderManager.getHolderColors();

            for (Iterator it = colors.keySet().iterator(); it.hasNext();) {
                String color = (String) it.next();
                String name = colors.get(color);
                setAttribute("color", color);
                setAttribute("name", name);
                renderFragment("color");
            }

            // Render source types sorted by value.
            renderFragment("outputFormHolderTypes");

            renderFragment("rowStart");

            DataHolderBuilder holderBuilder = dataHolderManager.getBuilderByBuilderType(Form.HOLDER_TYPE_CODE_POJO_DATA_MODEL);
            Map values = null;
            if (holderBuilder != null) values = holderBuilder.getOptions(wysiwygFormEditor.getCurrentEditionContext().getPath());

            renderSelectDataModel(Form.HOLDER_TYPE_CODE_POJO_DATA_MODEL,WysiwygFormEditor.PARAMETER_HOLDER_DM_INFO, sort(values, formHolderTypesComparator));

            renderFragment("rowEnd");

            renderFragment("rowStart");

            holderBuilder = dataHolderManager.getBuilderByBuilderType(Form.HOLDER_TYPE_CODE_BASIC_TYPE);
            values = null;
            if (holderBuilder != null) values = holderBuilder.getOptions(wysiwygFormEditor.getCurrentEditionContext().getPath());

            renderSelectDataModel(Form.HOLDER_TYPE_CODE_BASIC_TYPE,WysiwygFormEditor.PARAMETER_HOLDER_BT_INFO, sort(values, formHolderTypesComparator));

            renderFragment("rowEnd");

            renderFragment("outputFormAddHolderEnd");

            renderFragment("outputNameInput");

            renderFragment("outputStartBindings");

            int i=0;
            for (DataHolder holder : holders) {
                setAttribute("id", StringUtils.defaultString(holder.getUniqeId()));
                setAttribute("input_id", StringUtils.defaultString(holder.getInputId()));
                setAttribute("outId", StringUtils.defaultString(holder.getOuputId()));
                setAttribute("deleteId", StringUtils.defaultString(holder.getUniqeId()));
                setAttribute("type", holder.getTypeCode());
                setAttribute("renderColor", holder.getRenderColor());
                setAttribute("value", holder.getInfo());
                setAttribute("rowStyle",i%2==1 ? "skn-even_row":"skn-odd_row" );
                i++;
                renderFragment("outputBindings");
            }
            renderFragment("outputEndBindings");

            renderFragment("outputEnd");
        } catch (Exception e) {
            log.error("Error:", e);
        }
    }

    public void renderSelectDataModel(String id, String name, Map values) throws Exception {
        setAttribute("id", id);
        setAttribute("name", name);
        renderFragment("selectStart");

        if (values!= null ) {
            for (Iterator it = values.keySet().iterator(); it.hasNext();) {
                String key = (String) it.next();
                String value = (String) values.get(key);
                setAttribute("optionLabel", key);
                setAttribute("optionValue", value);
                renderFragment("selectOption");
            }
        }
        renderFragment("selectEnd");


    }

    public void renderPendingFields() throws Exception {
        WysiwygFormEditor wysiwygFormEditor = WysiwygFormEditor.lookup();
        Form form = wysiwygFormEditor.getCurrentForm();
        Set<DataHolder> holders = form.getHolders();

        renderFragment("outputStart");

        for (DataHolder dataHolder : holders) {
            String holderId = dataHolder.getUniqeId();//StringUtils.defaultIfEmpty(dataHolder.getInputId(), dataHolder.getOuputId());

            Set<DataFieldHolder> dataFieldHolders = dataHolder.getFieldHolders();

            String fieldName = "";
            int i = 0;
            if (dataFieldHolders != null) {
                for (DataFieldHolder dataFieldHolder : dataFieldHolders) {
                    fieldName = dataFieldHolder.getId();
                    if (fieldName != null && !form.isFieldBinded(dataHolder, fieldName)) {
                        if (i == 0) {//first field
                            setAttribute("id", dataHolder.getUniqeId());
                            setAttribute("type", dataHolder.getTypeCode());
                            setAttribute("renderColor", dataHolder.getRenderColor());

                            if (dataHolder.getUniqeId() != null && dataHolder.getUniqeId().equals(wysiwygFormEditor.getLastDataHolderUsedId())) {
                                setAttribute("open", Boolean.TRUE);
                            } else {
                                setAttribute("open", Boolean.FALSE);
                            }
                            String holderName = "";

                            holderName=dataHolder.getUniqeId();
                            if (holderName.length() > 20) holderName = holderName.substring(0, 19) + "...";

                            setAttribute("showHolderName", holderName);
                            if (Form.HOLDER_TYPE_CODE_BASIC_TYPE.equals(dataHolder.getTypeCode())){
                                setAttribute("noConfirm", Boolean.TRUE);
                            } else {
                                setAttribute("noConfirm", Boolean.FALSE);
                            }
                            renderFragment("outputBinding");

                        }
                        i++;
                        if (!Form.HOLDER_TYPE_CODE_BASIC_TYPE.equals(dataHolder.getTypeCode())){
                            renderAddField(fieldName, dataFieldHolder, holderId);
                        }
                    }
                }
                if (i != 0) {//last field of list
                    renderFragment("outputEndBinding");
                }
            }

        }

        renderFragment("outputEnd");
    }

    public void renderAddField(String fieldName, DataFieldHolder dataFieldHolder, String bindingId) {
        FieldType type = fieldTypeManager.getTypeByClass(dataFieldHolder.getClassName());

        if (type == null) return;

        setAttribute("className", dataFieldHolder.getClassName());
        setAttribute("typeName", type.getCode());
        setAttribute("bindingId", bindingId);
        setAttribute("showFieldName", ((fieldName != null && fieldName.length() < 17) ? fieldName : fieldName.substring(0, 13) + "..."));

        setAttribute("iconUri", fieldTypeManager.getIconPathForCode(type.getCode()));
        setAttribute("fieldName", fieldName);
        renderFragment("outputField");
    }

    /**
     * Sorts a map values using a given comparator implementation.
     *
     * @param source The source map.
     * @param c The comparator implementation. If <code>null</code>, the natural ordering of the keys will be used.
     * @return The sorted map or <code>null</code> if source map is <code>null</code>.
     */
    protected Map sort(Map source, Comparator<String> c) {
        if (source != null) {
            TreeMap sortedMap = new TreeMap(c);
            sortedMap.putAll(source);
            return sortedMap;
        }
        return null;
    }
}
