/*******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.air;

import com.ericsson.oss.air.api.generated.model.*;
import com.ericsson.oss.air.util.Serializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import jakarta.xml.bind.DatatypeConverter;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.generic.GenericRecord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Writer {

    public final String vintage;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    // adding static initialization objectMapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
    // to avoid null values in json
    static {
        objectMapper.configOverride (String.class).setInclude(
                (JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL)));
    }

    EricOssAssuranceIndexerIndexer openApiIndexer ;
    EricOssAssuranceIndexerWriter writerSpec;
    @Getter
    String schemaName;
    String topic;
    List<EricOssAssuranceIndexerContextField> contextFields;
    List<EricOssAssuranceIndexerValueField> valueFields;
    List<EricOssAssuranceIndexerInfoField> infoFields;

    public final String canonicalName;
    private String generateCanonicalName() {
        return "{" +
                "topic:" + topic +
                ",schema:" + schemaName +
                ",index:" + getIndexName() +
                ",v:[" + getValueFieldsSpec() +
                "],c:[" + getContextFieldsSpec() +
                "],f:[" + getInfoFieldsSpec() +
                "]}";
    }
    private String getValueFieldSpec(EricOssAssuranceIndexerValueField spec){
        if (spec.getRecordName() == null) {
            return spec.getName();
        }
        return spec.getRecordName()+"->"+spec.getName();
    }
    private String getContextFieldSpec(EricOssAssuranceIndexerContextField spec){
        if (spec.getRecordName() == null) {
            return spec.getName();
        }
        return spec.getRecordName()+"->"+spec.getName();
    }
    private String getInfoFieldSpec(EricOssAssuranceIndexerInfoField spec){
        if (spec.getRecordName() == null) {
            return spec.getName();
        }
        return spec.getRecordName()+"->"+spec.getName();
    }
    private String getValueFieldsSpec(){
        return valueFields.stream().map(this::getValueFieldSpec).collect(Collectors.joining(","));
    }
    private String getContextFieldsSpec(){
        return contextFields.stream().map(this::getContextFieldSpec).collect(Collectors.joining(","));
    }
    private String getInfoFieldsSpec(){
        return infoFields.stream().map(this::getInfoFieldSpec).collect(Collectors.joining(","));
    }
    private static String makeIdFromDocument(ObjectNode doc){
        // TODO: Refactor to use the origin record and not the Json document
        StringBuilder id = new StringBuilder(doc.get("value_name").asText());
        var contexts = new ArrayList<String>();
        for (var context : doc.get("context")) {
            contexts.add(context.asText());
        }
        Collections.sort(contexts);
        for (var context : contexts) {
            id.append("__").append(context).append("_").append(doc.get("c_" + context).asText());
        }
    return id.toString();
    }
    @SneakyThrows
    public Writer(EricOssAssuranceIndexerIndexer openApiIndexer, int writerNb) {
        log.debug("Constructor Writer: {}", writerNb);
        this.openApiIndexer = openApiIndexer;
        this.writerSpec = openApiIndexer.getWriters().get(writerNb);
        this.topic = openApiIndexer.getSource().getName();
        this.schemaName = this.writerSpec.getInputSchema();
        this.valueFields = this.writerSpec.getValue();
        this.contextFields = this.writerSpec.getContext();
        if (this.contextFields == null) {
            this.contextFields = Collections.emptyList();
        }
        this.infoFields = this.writerSpec.getInfo();
        if (this.infoFields == null) {
            this.infoFields = Collections.emptyList();
        }
        this.canonicalName = generateCanonicalName();
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(canonicalName.getBytes(StandardCharsets.UTF_8));
        this.vintage = DatatypeConverter.printHexBinary(md.digest());

        log.debug("Writer (vintage={}) created: {}", vintage, canonicalName);
    }

    private EricOssAssuranceIndexerValueDocumentSpec valueSpecToValueDocumentSpec(EricOssAssuranceIndexerValueField valueSpec, String valueDocumentName) {
        var valueDocumentSpec = new EricOssAssuranceIndexerValueDocumentSpec(valueSpec.getName(),"value_context",valueDocumentName);
        valueDocumentSpec.setDisplayName(valueSpec.getDisplayName());
        valueDocumentSpec.setUnit(valueSpec.getUnit());
        valueDocumentSpec.setType(valueSpec.getType());
        valueDocumentSpec.setDescription(valueSpec.getDescription());
        return valueDocumentSpec;
    }

    public static Object getField(GenericRecord record, String fieldName) {
        // We need to check if the record has the field we are looking for as we cannot relay on the
        // schema validation (the schema of the actual record might be different
        // from the schema used/assumed in the definition of the writer of IndexerSpec)
        try {
            return record.get(fieldName);
        } catch (AvroRuntimeException e) {
            log.error("{}. Likely, the AVRO record is of a different schema from the one assumed in the IndexerSpec.", e.getMessage());
            return null;
        }
    }
    public ObjectNode getDocument (EricOssAssuranceIndexerValueField vField, GenericRecord record) {
        log.debug("getDocument: {}", record);
        var document = objectMapper.createObjectNode();
        var vFieldName = vField.getName();
        var fullContext = computeContext(record, document);
        var valueContext = fullContext + "_" + vFieldName;
        var recordName = (vField.getRecordName() != null) ? vField.getRecordName() : vFieldName;
        var recordValue = getField(record, recordName);
        if (recordValue == null) {
            log.info("WARNING: Record '{}' has no value for field '{}'!", record, recordName);
            return null;
        }
        document.set("value_context", new TextNode(valueContext));
        document.set("value_name", new TextNode(vFieldName));

        var type = vField.getType();
        String valueDocumentName = null;
        switch (type) {
            case INTEGER:
                valueDocumentName = "vi_" + fullContext + "_" + vFieldName;
                document.set(valueDocumentName,
                        new LongNode(
                                ((recordValue instanceof Integer)
                                        ? (long) ((Integer) recordValue)
                                        : (Long) recordValue)
                        ));
                break;
            case FLOAT:
            default:
                valueDocumentName = "vd_" + fullContext + "_" + vFieldName;
                document.set(valueDocumentName, new DoubleNode((double) record.get(recordName)));
                break;
        }

        populateInfoFields(record, document);

        var xField = "xv_" + Serializer.shortHash(fullContext) + "_" + Serializer.shortHash(vFieldName) + "_" + Serializer.encode(valueSpecToValueDocumentSpec(vField, valueDocumentName));
        validateXField(xField, "xv_field", fullContext, vFieldName);
        document.set(xField, BooleanNode.TRUE );

        document.set("csac_table", new TextNode(this.writerSpec.getInputSchema()));
        document.set("csac_column", new TextNode(vField.getRecordName()));

        document.set("doc_id", new TextNode(makeIdFromDocument(document)));

        return document;
    }

    private void populateInfoFields(GenericRecord record, ObjectNode document) {
        log.debug("populateInfoFields: {}", record);
        for (EricOssAssuranceIndexerInfoField field : this.infoFields) {
            var fieldName = field.getName();
            var recordName = (field.getRecordName() != null) ? field.getRecordName() : fieldName;
            var type = field.getType();
            switch (type) {
                case TIME:
                    document.set("ft_"+fieldName, new LongNode((long) record.get(recordName)));
                    break;
                case STRING:
                default:
                    document.set("fk_"+fieldName, new TextNode(record.get(recordName).toString()));
            }
        }
    }

    @SneakyThrows
    private String computeContext(GenericRecord record, ObjectNode document) {
        log.debug("computeContext: {}", record);
        List<String> actualContextFieldNames = new ArrayList<>();
        var contextMap = new HashMap<String,EricOssAssuranceIndexerContextField>();
        for (EricOssAssuranceIndexerContextField field : this.contextFields) {
            var fieldName = field.getName();
            var recordName = (field.getRecordName() != null) ? field.getRecordName() : fieldName;
            var fieldValue = record.get(recordName).toString();
            var type = field.getNameType();
            var actualFieldName = fieldName;
            var actualFieldValue = fieldValue;
            switch (type) {
                case COLONSEPARATED: {
                    var a = fieldValue.split(":", 2);
                    assert (a.length == 2);
                    actualFieldName = a[0];
                    actualFieldValue = a[1];
                    break;
                }
                case STRAIGHT:
                default:
                    break;
            }
            actualContextFieldNames.add(actualFieldName);
            document.set("c_" + actualFieldName, new TextNode(actualFieldValue));
            contextMap.put(actualFieldName, field);
        }
        Collections.sort(actualContextFieldNames);
        var fullContext = String.join("_", actualContextFieldNames );
        document.set("full_context", new TextNode(fullContext));

        var arrayNode = objectMapper.createArrayNode();
        arrayNode.addAll(actualContextFieldNames.stream().map(TextNode::new).collect(Collectors.toList()));
        document.set("context", arrayNode);

        var xContext = new EricOssAssuranceIndexerFullContext(fullContext, new ArrayList<>());
        for( var fieldName: actualContextFieldNames) {
            var cc = contextMap.get(fieldName);
            var contextItem = new EricOssAssuranceIndexerContextFieldSpec(fieldName ,"c_"+fieldName);
            contextItem.setDescription(cc.getDescription());
            contextItem.setDisplayName(cc.getDisplayName());
            xContext.getContext().add(contextItem);
        }

        var xField = "xc_"+ Serializer.encode(xContext);
        validateXField(xField, "xc_field", fullContext);
        document.set(xField, BooleanNode.TRUE);

        return fullContext;
    }

    public List<ObjectNode> recordToDocuments(final GenericRecord record) {
        List<ObjectNode> documents = new ArrayList<>();
        for (EricOssAssuranceIndexerValueField field : this.valueFields) {
            var document = getDocument(field, record);
            if (document != null) {
                documents.add(document);
            }
        }
        return documents;
    }

    public String getIndexName() {
        return openApiIndexer.getTarget().getName();
    }

    @Override
    public String toString(){
        return canonicalName;
    }

    static void validateXField(String fieldName, String... context) {
        if (fieldName.length() > 2048) {
            var extraInfo = String.join(" : ", context);
            throw new IllegalArgumentException("Generated xField is too long! Debug info: " + extraInfo);
        }
    }
}
