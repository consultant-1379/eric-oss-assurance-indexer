/*******************************************************************************
 * COPYRIGHT Ericsson 2021
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


package com.ericsson.oss.air.ais.contract;

import com.ericsson.oss.air.api.generated.model.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerContextField.NameTypeEnum.STRAIGHT;
import static com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerDataSource.TypeEnum.PMSTATSEXPORTER;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IndexTestUtil {

  public static final String INDEX_NAME = "nameOfIndexerA";
  public static final String INDEX_NAME_B = "indexerB";
  public static final String INVALID_INDEX_NAME = "invalidIndexName";
  public static final String INVALID_SEARCH_ENGINE_INDEX = "invalidSearchEngineIndexName";
  public static final String INVALID_FULL_CONTEXT_NAME = "invalidContext";
  public static final String INDEX_DESCRIPTION = "description for indexer A";
  public static final String INDEX_DESCRIPTION_B = "description of indexer B";
  public static final String INDEX_SOURCE_NAME = "KafkaTopicName";
  public static final EricOssAssuranceIndexerDataSource.TypeEnum INDEX_SOURCE_TYPE = PMSTATSEXPORTER;
  public static final String INDEX_TARGET_NAME = "assurance-search_index_a_name";
  public static final String INDEX_TARGET_DISPLAY_NAME = "SearchIndexA_DisplayName";
  public static final String INDEX_TARGET_INDEX_DESCRIPTION = "SearchIndexA_Description";
  public static final String INDEX_WRITER_NAME = "writerA_name";
  public static final String INDEX_WRITER_INPUT_SCHEMA = "writerA_schemaRegistryName";
  public static final String INDEX_WRITER_CONTEXT_NAME = "contextFieldA_name";
  public static final String INDEX_WRITER_CONTEXT_DISPLAY_NAME = "Context Field A";
  public static final EricOssAssuranceIndexerContextField.NameTypeEnum INDEX_WRITER_CONTEXT_NAME_TYPE = STRAIGHT;
  public static final String INDEX_WRITER_CONTEXT_RECORD_NAME = "contextFieldA_recordName";
  public static final String INDEX_WRITER_CONTEXT_DESCRIPTION = "contextFieldA Description";
  public static final String INDEX_WRITER_VALUE_NAME = "valueFieldA_name";
  public static final String INDEX_WRITER_VALUE_DISPLAY_NAME = "Value Field A";
  public static final String INDEX_WRITER_VALUE_UNIT = "errors/minute";
  public static final EricOssAssuranceIndexerValueFieldType INDEX_WRITER_VALUE_TYPE = EricOssAssuranceIndexerValueFieldType.FLOAT;
  public static final String INDEX_WRITER_VALUE_RECORD_NAME = "valueFieldA_recordName";
  public static final String INDEX_WRITER_VALUE_DESCRIPTION = "valueFieldA Description";
  public static final String INDEX_WRITER_INFO_NAME = "infoFieldA_name";
  public static final String INDEX_WRITER_INFO_DISPLAY_NAME = "Info Field A";
  public static final EricOssAssuranceIndexerInfoFieldType INDEX_WRITER_INFO_TYPE = EricOssAssuranceIndexerInfoFieldType.STRING;
  public static final String INDEX_WRITER_INFO_RECORD_NAME = "infoFieldA_recordName";
  public static final String INDEX_WRITER_INFO_DESCRIPTION = "infoFieldA Description";
  public static final String SEARCH_ENGINE_INDEX = "assurance-searchEngineIndexName";
  public static final String FULL_CONTEXT_DOCUMENT_NAME = "full_context";
  public static final String FULL_CONTEXT_NAME_A = "Context4_c1";
  public static final String CONTEXT_ITEM_NAME_A1 = "c1";
  public static final String CONTEXT_ITEM_DISPLAY_A1 = "Context1_DisplayName";
  public static final String CONTEXT_ITEM_DOCUMENT_A1 = "c_c1";
  public static final String CONTEXT_ITEM_DESCRIPTION_A1 = "Context1_Description";
  public static final String CONTEXT_ITEM_NAME_A2 = "Context4";
  public static final String CONTEXT_ITEM_DISPLAY_A2 = "Context2_DisplayName";
  public static final String CONTEXT_ITEM_DOCUMENT_A2 = "c_Context4";
  public static final String CONTEXT_ITEM_DESCRIPTION_A2 = null;
  public static final String FULL_CONTEXT_NAME_B = "Context0_c1";
  public static final String CONTEXT_ITEM_NAME_B1 = "c1";
  public static final String CONTEXT_ITEM_DISPLAY_B1 = "Context1_DisplayName";
  public static final String CONTEXT_ITEM_DOCUMENT_B1 = "c_c1";
  public static final String CONTEXT_ITEM_DESCRIPTION_B1 = "Context1_Description";
  public static final String CONTEXT_ITEM_NAME_B2 = "Context0";
  public static final String CONTEXT_ITEM_DISPLAY_B2 = "Context2_DisplayName";
  public static final String CONTEXT_ITEM_DOCUMENT_B2 = "c_Context0";
  public static final String CONTEXT_ITEM_DESCRIPTION_B2 = null;
  public static final String SEARCH_ENGINE_INDEX_NAME = "assurance-soa";
  public static final String SEARCH_ENGINE_INDEX_DISPLAY_NAME = "Display Name for 'SOA' index.";
  public static final String SEARCH_ENGINE_INDEX_DESCRIPTION = "SOA index description";
  public static final String FULL_CONTEXT_VALUES_VALUE_NAME_A = "valueFieldX-name";
  public static final String FULL_CONTEXT_VALUES_VALUE_DISPLAY_NAME_A = "Context Field A";
  public static final String FULL_CONTEXT_VALUES_VALUE_UNIT_A = "errors/minute";
  public static final EricOssAssuranceIndexerValueFieldType FULL_CONTEXT_VALUES_VALUE_TYPE_A = EricOssAssuranceIndexerValueFieldType.FLOAT;
  public static final String FULL_CONTEXT_VALUES_VALUE_CONTEXT_DOCUMENT_NAME_A = "value_context";
  public static final String FULL_CONTEXT_VALUES_VALUE_DOCUMENT_NAME_A = "vd_ContextA_ContextB_valueFieldX-name";
  public static final String FULL_CONTEXT_VALUES_DESCRIPTION_A = "valueFieldX Description";
  public static final String FULL_CONTEXT_VALUES_VALUE_NAME_B  = "vi";
  public static final String FULL_CONTEXT_VALUES_VALUE_DISPLAY_NAME_B = null;
  public static final String FULL_CONTEXT_VALUES_VALUE_UNIT_B = null;
  public static final String FULL_CONTEXT_VALUES_VALUE_CONTEXT_DOCUMENT_NAME_B = "value_context";
  public static final String FULL_CONTEXT_VALUES_VALUE_DOCUMENT_NAME_B = "vi_Context1_c1_vi";
  public static final String FULL_CONTEXT_VALUES_DESCRIPTION_B = "value description for vi";
  public static final EricOssAssuranceIndexerValueFieldType FULL_CONTEXT_VALUES_VALUE_TYPE_B = EricOssAssuranceIndexerValueFieldType.INTEGER;

  public static final EricOssAssuranceIndexerIndexer INDEX_RESPONSE = new EricOssAssuranceIndexerIndexer()
          .name(INDEX_NAME)
          .description(INDEX_DESCRIPTION)
          .source(new EricOssAssuranceIndexerDataSource()
                  .name(INDEX_SOURCE_NAME)
                  .type(INDEX_SOURCE_TYPE))
          .target(new EricOssAssuranceIndexerSearchEngineIndex()
                  .name(INDEX_TARGET_NAME)
                  .displayName(INDEX_TARGET_DISPLAY_NAME)
                  .indexDescription(INDEX_TARGET_INDEX_DESCRIPTION))
          .addWritersItem(new EricOssAssuranceIndexerWriter()
                  .name(INDEX_WRITER_NAME)
                  .inputSchema(INDEX_WRITER_INPUT_SCHEMA)
                  .addContextItem(new EricOssAssuranceIndexerContextField()
                          .name(INDEX_WRITER_CONTEXT_NAME)
                          .displayName(INDEX_WRITER_CONTEXT_DISPLAY_NAME)
                          .nameType(INDEX_WRITER_CONTEXT_NAME_TYPE)
                          .recordName(INDEX_WRITER_CONTEXT_RECORD_NAME)
                          .description(INDEX_WRITER_CONTEXT_DESCRIPTION))
                  .addValueItem(new EricOssAssuranceIndexerValueField()
                          .name(INDEX_WRITER_VALUE_NAME)
                          .displayName(INDEX_WRITER_VALUE_DISPLAY_NAME)
                          .unit(INDEX_WRITER_VALUE_UNIT)
                          .type(INDEX_WRITER_VALUE_TYPE)
                          .recordName(INDEX_WRITER_VALUE_RECORD_NAME)
                          .description(INDEX_WRITER_VALUE_DESCRIPTION))
                  .addInfoItem(new EricOssAssuranceIndexerInfoField()
                          .name(INDEX_WRITER_INFO_NAME)
                          .displayName(INDEX_WRITER_INFO_DISPLAY_NAME)
                          .type(INDEX_WRITER_INFO_TYPE)
                          .recordName(INDEX_WRITER_INFO_RECORD_NAME)
                          .description(INDEX_WRITER_INFO_DESCRIPTION)));

  public static final EricOssAssuranceIndexerFullContextSpec FULL_CONTEXT_RESPONSE = new EricOssAssuranceIndexerFullContextSpec()
          .documentName(FULL_CONTEXT_DOCUMENT_NAME)
          .addFullContextItem(new EricOssAssuranceIndexerFullContext()
                  .name(FULL_CONTEXT_NAME_A)
                  .addContextItem(new EricOssAssuranceIndexerContextFieldSpec()
                          .name(CONTEXT_ITEM_NAME_A1)
                          .displayName(CONTEXT_ITEM_DISPLAY_A1)
                          .documentName(CONTEXT_ITEM_DOCUMENT_A1)
                          .description(CONTEXT_ITEM_DESCRIPTION_A1))
                  .addContextItem(new EricOssAssuranceIndexerContextFieldSpec()
                          .name(CONTEXT_ITEM_NAME_A2)
                          .displayName(CONTEXT_ITEM_DISPLAY_A2)
                          .documentName(CONTEXT_ITEM_DOCUMENT_A2)
                          .description(CONTEXT_ITEM_DESCRIPTION_A2)))
          .addFullContextItem(new EricOssAssuranceIndexerFullContext()
                  .name(FULL_CONTEXT_NAME_B)
                  .addContextItem(new EricOssAssuranceIndexerContextFieldSpec()
                          .name(CONTEXT_ITEM_NAME_B1)
                          .displayName(CONTEXT_ITEM_DISPLAY_B1)
                          .documentName(CONTEXT_ITEM_DOCUMENT_B1)
                          .description(CONTEXT_ITEM_DESCRIPTION_B1))
                  .addContextItem(new EricOssAssuranceIndexerContextFieldSpec()
                          .name(CONTEXT_ITEM_NAME_B2)
                          .displayName(CONTEXT_ITEM_DISPLAY_B2)
                          .documentName(CONTEXT_ITEM_DOCUMENT_B2)
                          .description(CONTEXT_ITEM_DESCRIPTION_B2)));

  public static final EricOssAssuranceIndexerValueContextSpec FULL_CONTEXT_VALUES_RESPONSE = new EricOssAssuranceIndexerValueContextSpec()
          .addValueItem(new EricOssAssuranceIndexerValueDocumentSpec()
                  .name(FULL_CONTEXT_VALUES_VALUE_NAME_A)
                  .displayName(FULL_CONTEXT_VALUES_VALUE_DISPLAY_NAME_A)
                  .unit(FULL_CONTEXT_VALUES_VALUE_UNIT_A)
                  .type(FULL_CONTEXT_VALUES_VALUE_TYPE_A)
                  .valueContextDocumentName(FULL_CONTEXT_VALUES_VALUE_CONTEXT_DOCUMENT_NAME_A)
                  .valueDocumentName(FULL_CONTEXT_VALUES_VALUE_DOCUMENT_NAME_A)
                  .description(FULL_CONTEXT_VALUES_DESCRIPTION_A))
          .addValueItem(new EricOssAssuranceIndexerValueDocumentSpec()
                  .name(FULL_CONTEXT_VALUES_VALUE_NAME_B)
                  .displayName(FULL_CONTEXT_VALUES_VALUE_DISPLAY_NAME_B)
                  .unit(FULL_CONTEXT_VALUES_VALUE_UNIT_B)
                  .type(FULL_CONTEXT_VALUES_VALUE_TYPE_B)
                  .valueContextDocumentName(FULL_CONTEXT_VALUES_VALUE_CONTEXT_DOCUMENT_NAME_B)
                  .valueDocumentName(FULL_CONTEXT_VALUES_VALUE_DOCUMENT_NAME_B)
                  .description(FULL_CONTEXT_VALUES_DESCRIPTION_B));

  public static final EricOssAssuranceIndexerSearchEngineIndex SEARCH_ENGINE_LIST_RESPONSE = new EricOssAssuranceIndexerSearchEngineIndex()
          .name(SEARCH_ENGINE_INDEX_NAME)
          .displayName(SEARCH_ENGINE_INDEX_DISPLAY_NAME)
          .indexDescription(SEARCH_ENGINE_INDEX_DESCRIPTION);

  public static final EricOssAssuranceIndexerIndexerRef INDEX_REF_A = new EricOssAssuranceIndexerIndexerRef()
          .name(INDEX_NAME)
          .description(INDEX_DESCRIPTION);

  public static final EricOssAssuranceIndexerIndexerRef INDEX_REF_B = new EricOssAssuranceIndexerIndexerRef()
          .name(INDEX_NAME_B)
          .description(INDEX_DESCRIPTION_B);
}
