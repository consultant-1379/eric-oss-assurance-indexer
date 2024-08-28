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

package com.ericsson.oss.air.controller;

import com.ericsson.adp.security.certm.certificatewatcher.CertificateWatcherService;
import com.ericsson.oss.air.IndexerDB;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static com.fasterxml.jackson.databind.node.BooleanNode.valueOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class IndexerSpecValidationTest {
  @MockBean
  private CertificateWatcherService certificateWatcherService;

  @MockBean
  private IndexerDB noIndexerDB;

  ObjectMapper mapper = new ObjectMapper();

  @Autowired
  private MockMvc mockMvc;

  private static File indexFile;
  private JsonNode indexerSpec;

  @BeforeAll
  public static void initializeIndexFile() {
    indexFile = new File("./src/test/resources/json-files/indexer-spec-full.json");
  }

  @BeforeEach
  public void initializeIndexerSpec() throws IOException{
    indexerSpec = mapper.readTree(indexFile);
  }

  /**********************/
  /***** Root Index *****/
  /**********************/

  @Test
  public void given_valid_index_should_respond_OK() throws Exception {
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

    @Test
  public void given_valid_index_should_respond_OK_With_TrailingSlash() throws Exception {
    mockMvc.perform(post("/v1/indexer-info/indexer/")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  @Test
  public void given_index_with_only_required_fields_should_respond_OK() throws Exception {
    if (indexerSpec.has("description"))
      ((ObjectNode)indexerSpec).remove("description");
    if (indexerSpec.has("source") && indexerSpec.path("source").has("type"))
      ((ObjectNode)indexerSpec.path("source")).remove("type");
    if (indexerSpec.has("target") && indexerSpec.path("target").has("displayName"))
      ((ObjectNode)indexerSpec.path("target")).remove("displayName");
    if (indexerSpec.has("target") && indexerSpec.path("target").has("indexDescription"))
      ((ObjectNode)indexerSpec.path("target")).remove("indexDescription");
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      Iterator<JsonNode> writerIter = indexerSpec.path("writers").elements();
      var writer = writerIter.next();
      // remove all but the first writer for testing
      while(writerIter.hasNext()) {
        writerIter.next();
        writerIter.remove();
      }
      if (writer.has("context"))
        ((ObjectNode)writer).remove("context");
      if (writer.has("info"))
        ((ObjectNode)writer).remove("info");
      if (writer.has("value") && writer.path("value").isArray()) {
        Iterator<JsonNode> valueIter = writer.path("value").elements();
        var value = valueIter.next();
        // remove all but the first value for testing
        while(valueIter.hasNext()) {
          valueIter.next();
          valueIter.remove();
        }
        if (value.has("displayName"))
          ((ObjectNode)value).remove("displayName");
        if (value.has("unit"))
          ((ObjectNode)value).remove("unit");
        if (value.has("type"))
          ((ObjectNode)value).remove("type");
        if (value.has("recordName"))
          ((ObjectNode)value).remove("recordName");
        if (value.has("description"))
          ((ObjectNode)value).remove("description");
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  @Test
  public void given_index_with_only_required_fields_and_writer_context_and_info_fields_should_respond_OK() throws Exception {
    if (indexerSpec.has("description"))
      ((ObjectNode)indexerSpec).remove("description");
    if (indexerSpec.has("source") && indexerSpec.path("source").has("type"))
      ((ObjectNode)indexerSpec.path("source")).remove("type");
    if (indexerSpec.has("target") && indexerSpec.path("target").has("displayName"))
      ((ObjectNode)indexerSpec.path("target")).remove("displayName");
    if (indexerSpec.has("target") && indexerSpec.path("target").has("indexDescription"))
      ((ObjectNode)indexerSpec.path("target")).remove("indexDescription");
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      Iterator<JsonNode> writerIter = indexerSpec.path("writers").elements();
      var writer = writerIter.next();
      // remove all but the first writer for testing
      while(writerIter.hasNext()) {
        writerIter.next();
        writerIter.remove();
      }
      if (writer.has("context") && writer.path("context").isArray()) {
        Iterator<JsonNode> contextIter = writer.path("context").elements();
        var context = contextIter.next();
        // remove all but the first context for testing
        while(contextIter.hasNext()) {
          contextIter.next();
          contextIter.remove();
        }
        if (context.has("displayName"))
          ((ObjectNode)context).remove("displayName");
        if (context.has("nameType"))
          ((ObjectNode)context).remove("nameType");
        if (context.has("recordName"))
          ((ObjectNode)context).remove("recordName");
        if (context.has("description"))
          ((ObjectNode)context).remove("description");
      }
      if (writer.has("info") && writer.path("info").isArray()) {
        Iterator<JsonNode> infoIter = writer.path("info").elements();
        var info = infoIter.next();
        // remove all but the first context for testing
        while(infoIter.hasNext()) {
          infoIter.next();
          infoIter.remove();
        }
        if (info.has("displayName"))
          ((ObjectNode)info).remove("displayName");
        if (info.has("type"))
          ((ObjectNode)info).remove("type");
        if (info.has("recordName"))
          ((ObjectNode)info).remove("recordName");
        if (info.has("description"))
          ((ObjectNode)info).remove("description");
      }
      if (writer.has("value") && writer.path("value").isArray()) {
        Iterator<JsonNode> valueIter = writer.path("value").elements();
        var value = valueIter.next();
        // remove all but the first value for testing
        while(valueIter.hasNext()) {
          valueIter.next();
          valueIter.remove();
        }
        if (value.has("displayName"))
          ((ObjectNode)value).remove("displayName");
        if (value.has("unit"))
          ((ObjectNode)value).remove("unit");
        if (value.has("type"))
          ((ObjectNode)value).remove("type");
        if (value.has("recordName"))
          ((ObjectNode)value).remove("recordName");
        if (value.has("description"))
          ((ObjectNode)value).remove("description");
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  @Test
  public void given_index_with_no_name_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("name"))
      ((ObjectNode)indexerSpec).remove("name");
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_empty_name_should_respond_BAD_REQUEST() throws Exception {
    ((ObjectNode)indexerSpec).set("name", JsonNodeFactory.instance.textNode(""));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_name_with_special_characters_should_respond_OK() throws Exception {
    ((ObjectNode)indexerSpec).set("name", JsonNodeFactory.instance.textNode("[a-zA-Z0-9-_!/.,;:?@#%^&*()|`~\\{}]+"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  /******************/
  /***** Source *****/
  /******************/

  @Test
  public void given_index_with_no_source_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("source"))
      ((ObjectNode)indexerSpec).remove("source");
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_no_source_name_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("source") && indexerSpec.path("source").has("name"))
      ((ObjectNode)indexerSpec.path("source")).remove("name");
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  /******************/
  /***** Target *****/
  /******************/

  @Test
  public void given_index_with_no_target_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode) indexerSpec).remove("target");
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }
  @Test
  public void given_index_with_no_target_name_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target") && indexerSpec.path("target").has("name"))
      ((ObjectNode)indexerSpec.path("target")).remove("name");
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_empty_target_name_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode(""));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  /******************/
  /***** Writer *****/
  /******************/

  @Test
  public void given_index_with_no_writers_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers"))
      ((ObjectNode)indexerSpec).remove("writers");
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_empty_writers_array_should_respond_BAD_REQUEST() throws Exception {
    ((ObjectNode) indexerSpec).putArray("writers");
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  // TODO: Implement correct validation in order for this test to pass
//  @Test
  public void given_index_with_null_item_in_writer_array_should_respond_BAD_REQUEST() throws Exception {
    JsonNode node = null;
    ((ArrayNode) indexerSpec.get("writers")).add(node);
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_null_writers_should_respond_BAD_REQUEST() throws Exception {
    JsonNode node = null;
    ((ObjectNode) indexerSpec).set("writers", node);
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_no_writer_name_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("name"))
        ((ObjectNode) writer).remove("name");
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_no_writer_inputSchema_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("inputSchema"))
        ((ObjectNode) writer).remove("inputSchema");
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  /**************************/
  /***** Writer Context *****/
  /**************************/

  @Test
  public void given_index_with_no_writer_context_name_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("context") && writer.path("context").isArray()) {
        var context = writer.get("context").get(0);
        if (context.has("name"))
          ((ObjectNode)context).remove("name");
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_no_writer_context_should_respond_OK() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("context"))
          ((ObjectNode)writer).remove("context");
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  @Test
  public void given_index_with_empty_writer_context_array_should_respond_OK() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      ((ObjectNode) writer).putArray("context");
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  // TODO: Implement correct validation in order for this test to pass
//  @Test
  public void given_index_with_null_item_in_writer_context_array_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("context") && writer.path("context").isArray()) {
        JsonNode node = null;
        ((ArrayNode) writer.get("context")).add(node);
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_null_writer_context_should_respond_OK() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      JsonNode node = null;
      ((ObjectNode) writer).set("context", node);
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  /*************************/
  /***** Writer Values *****/
  /*************************/

  @Test
  public void given_index_with_no_writer_value_name_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("value") && writer.path("value").isArray()) {
        var value = writer.get("value").get(0);
        if (value.has("name"))
          ((ObjectNode) value).remove("name");
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_no_writer_values_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("value"))
        ((ObjectNode) writer).remove("value");
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_empty_writer_value_array_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      ((ObjectNode) writer).putArray("value");
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  // TODO: Implement correct validation in order for this test to pass
//  @Test
  public void given_index_with_null_item_in_writer_value_array_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("value") && writer.path("value").isArray()) {
        JsonNode node = null;
        ((ArrayNode) writer.get("value")).add(node);
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_null_writer_value_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      JsonNode node = null;
      ((ObjectNode) writer).set("value", node);
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  /***********************/
  /***** Writer Info *****/
  /***********************/

  @Test
  public void given_index_with_no_writer_info_name_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("info") && writer.path("info").isArray()) {
        var info = writer.get("info").get(0);
        if (info.has("name"))
          ((ObjectNode) info).remove("name");
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_no_writer_info_should_respond_OK() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("info"))
        ((ObjectNode)writer).remove("info");
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  @Test
  public void given_index_with_empty_writer_info_array_should_respond_OK() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      ((ObjectNode) writer).putArray("info");
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  // TODO: Implement correct validation in order for this test to pass
//  @Test
  public void given_index_with_null_item_in_writer_info_array_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      if (writer.has("info") && writer.path("info").isArray()) {
        JsonNode node = null;
        ((ArrayNode) writer.get("info")).add(node);
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_null_writer_info_should_respond_OK() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      var writer = indexerSpec.get("writers").get(0);
      JsonNode node = null;
      ((ObjectNode) writer).set("info", node);
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  /****************************/
  /***** Wrong data types *****/
  /****************************/

  @Test
  public void given_index_with_name_as_object_should_respond_BAD_REQUEST() throws Exception {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.set("test", JsonNodeFactory.instance.textNode("object"));
    ((ObjectNode) indexerSpec).set("name", node);
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_description_as_object_should_respond_BAD_REQUEST() throws Exception {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.set("test", JsonNodeFactory.instance.textNode("object"));
    ((ObjectNode) indexerSpec).set("description", node);
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  /*** Source with type tests ***/
  @Test
  public void given_index_with_source_as_array_should_respond_BAD_REQUEST() throws Exception {
    ((ObjectNode) indexerSpec).putArray("source");
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_source_name_as_object_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("source")) {
      ObjectNode node = JsonNodeFactory.instance.objectNode();
      node.set("test", JsonNodeFactory.instance.textNode("object"));
      ((ObjectNode) indexerSpec.path("source")).set("name", node);
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_wrong_source_type_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("source"))
      ((ObjectNode)indexerSpec.path("source")).set("type", JsonNodeFactory.instance.textNode("badType"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  /*** Invalid Target Name Tests ***/
  @Test
  public void given_index_with_capital_letters_in_target_name_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.path("target")).set("name", JsonNodeFactory.instance.textNode("TARGET_NAME"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_valid_special_characters_should_respond_OK() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target.name;with-special_characters=!@%&(){}[]`~123"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  @Test
  public void given_index_target_name_with_starting_dash_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("-target_name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_starting_underscore_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("_target_name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_comma_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target,name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_colon_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target:name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_backslash_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target\\name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_double_quote_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target\"name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_asterisk_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target*name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_plus_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target+name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_forward_slash_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target/name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_pipe_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target|name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_question_mark_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target?name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_hash_mark_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target#name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_greater_than_symbol_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target>name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_less_than_symbol_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target<name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_with_space_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target"))
      ((ObjectNode)indexerSpec.get("target")).set("name", JsonNodeFactory.instance.textNode("target name"));
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  /*** Writer as Wrong Type Tests ***/
  @Test
  public void given_index_with_writers_as_object_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      JsonNode writer = indexerSpec.get("writers").get(0);
      ((ObjectNode) indexerSpec).set("writers", writer);
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_wrong_writers_context_nameType_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      JsonNode writer = indexerSpec.path("writers").get(0);
      if (writer.has("context") && writer.path("context").isArray()) {
        ((ObjectNode) writer.get("context").get(0)).set("nameType", JsonNodeFactory.instance.textNode("badType"));
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_wrong_writers_value_type_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      JsonNode writer = indexerSpec.path("writers").get(0);
      if (writer.has("value") && writer.path("value").isArray()) {
        ((ObjectNode) writer.get("value").get(0)).set("type", JsonNodeFactory.instance.textNode("badType"));
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_with_wrong_writers_info_type_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("writers") && indexerSpec.path("writers").isArray()) {
      JsonNode writer = indexerSpec.path("writers").get(0);
      if (writer.has("info") && writer.path("info").isArray()) {
        ((ObjectNode) writer.get("info").get(0)).set("type", JsonNodeFactory.instance.textNode("badType"));
      }
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  /************************/
  /***** Missing Body *****/
  /************************/

  @Test
  public void given_index_with_empty_json_object_should_respond_BAD_REQUEST() throws Exception {
    indexerSpec = JsonNodeFactory.instance.objectNode();
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_empty_index_body_should_respond_BAD_REQUEST() throws Exception {
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(""))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_no_body_should_respond_BAD_REQUEST() throws Exception {
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
  }

  /************************/
  /***** Extra Fields *****/
  /************************/

  @Test
  public void given_index_with_extra_field_should_respond_OK() throws Exception {
    JsonNode node = JsonNodeFactory.instance.textNode("field");
    ((ObjectNode) indexerSpec).set("extra", node);
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  @Test
  public void given_index_with_extra_field_in_source_should_respond_OK() throws Exception {
    if (indexerSpec.has("source")) {
      JsonNode node = JsonNodeFactory.instance.textNode("field");
      ((ObjectNode) indexerSpec.get("source")).set("extra", node);
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  @Test
  public void given_index_with_extra_object_should_respond_OK() throws Exception {
    ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
    JsonNode node = JsonNodeFactory.instance.textNode("field");
    objectNode.set("extra", node);
    ((ObjectNode) indexerSpec).set("testObject", objectNode);
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  @Test
  public void given_index_with_extra_array_should_respond_OK() throws Exception {
    ((ObjectNode) indexerSpec).putArray("array");
    ((ArrayNode) indexerSpec.get("array")).add("test");
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isOk());
  }

  /*************************/
  /***** String Length *****/
  /*************************/

  @Test
  public void given_index_name_too_long_should_respond_BAD_REQUEST() throws Exception {
    String longName = "a";
    longName = longName.repeat(513);
    JsonNode node = JsonNodeFactory.instance.textNode(longName);
    ((ObjectNode) indexerSpec).set("name", node);
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }

  @Test
  public void given_index_target_name_too_long_should_respond_BAD_REQUEST() throws Exception {
    if (indexerSpec.has("target")) {
      String longName = "a";
      longName = longName.repeat(256);
      JsonNode node = JsonNodeFactory.instance.textNode(longName);
      ((ObjectNode) indexerSpec.path("target")).set("name", node);
    }
    mockMvc.perform(post("/v1/indexer-info/indexer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(indexerSpec.toString()))
            .andExpect(status().isBadRequest());
  }
}
