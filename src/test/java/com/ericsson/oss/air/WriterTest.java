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

package com.ericsson.oss.air;

import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerContextFieldSpec;
import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerFullContext;
import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerIndexer;
import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerValueDocumentSpec;
import com.ericsson.oss.air.util.Serializer;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.SneakyThrows;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.generic.GenericRecordBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WriterTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode documentsSpec;
    private static File documentsFile;
    @Test
    public void recordToDocuments_test() throws IOException {
        var schema1 = new Schema.Parser().parse(new File("./src/test/resources/avro-schemas/schema1.avsc"));
        var record = new GenericRecordBuilder(schema1)
                .set("c1", "context 1...")
                .set("c2", "XXX:generated")
                .set("rvi", 10)
                .set("rvf", 99.2)
                .set("ri1", "info 1: ?")
                .set("ri2", "info 2: ?")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        var openApiIndexer = mapper.readValue(
                new File("./src/test/resources/json-files/indexer.json"),
                EricOssAssuranceIndexerIndexer.class);

        try {
            var writer = new Writer(openApiIndexer, 0);
            System.out.printf("Writer spec: '%s'%n", writer);
            var documents = writer.recordToDocuments(record);
            System.out.println(documents);
            // Assertions.assertTrue(result.getResponse().getContentAsString().contains("jvm_threads_states_threads"));
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }
    @Test
    public void recordToDocuments_infoField_time() throws IOException {
        var schema = new Schema.Parser().parse(new File("./src/test/resources/avro-schemas/schema3.avsc"));
        var record = new GenericRecordBuilder(schema)
                .set("SNSSAI", "1:1")
                .set("NF", "AMF_BC")
                .set("Collection", "SITE:BC")
                .set("csac_0fcf6508_67cc_4969_1f2f_566c106e38b0", 10.0)
                .set("csac_9a6ec349_5637_4c92_8bfd_a55630f442d5", 99)
                .set("aggregation_begin_time", 1212133444L)
                .set("aggregation_end_time", 1212133455L)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        var openApiIndexer = mapper.readValue(
                new File("./src/test/resources/json-files/indexer-spec-3.json"),
                EricOssAssuranceIndexerIndexer.class);

        try {
            var writer = new Writer(openApiIndexer, 0);
            System.out.printf("Writer spec: '%s'%n", writer);
            var documents = writer.recordToDocuments(record);
            var expectedDocument = objectMapper.createObjectNode();

            expectedDocument.set("c_SNSSAI", new TextNode("1:1"));
            expectedDocument.set("c_NF", new TextNode("AMF_BC"));
            expectedDocument.set("c_SITE", new TextNode("BC"));
            expectedDocument.set("doc_id", new TextNode("AMFMeanRegNbr__NF_AMF_BC__SITE_BC__SNSSAI_1:1"));

            expectedDocument.set("full_context", new TextNode("NF_SITE_SNSSAI"));
            var arrayNode = objectMapper.createArrayNode();
            arrayNode.add("NF").add("SITE").add("SNSSAI");
            expectedDocument.set("context", arrayNode);
            expectedDocument.set(
                    "xc_eJyrVspLzE1VslLyc4sP9gxxjQ022Cw529FTSUUrOzytJrShRsoquRqgBiqdkFhfkJFb6gYXySnNygEL5yaW5qXklEDGl5HiIwtTi5KLMgpLM02DyQIMQ4t8zUnBQFPzcFZNlaHbgVIDcQaQlMKW5rnPNzclKTQTI4rYP5ligL4YpxWwlRg2pdbC01Af03N6ig0404",
                    BooleanNode.TRUE);

            expectedDocument.set("value_context", new TextNode("NF_SITE_SNSSAI_AMFMeanRegNbr"));
            expectedDocument.set("value_name", new TextNode("AMFMeanRegNbr"));
            expectedDocument.set("vd_NF_SITE_SNSSAI_AMFMeanRegNbr",new DoubleNode(10.0));
            expectedDocument.set("ft_begin_timestamp", new LongNode(1212133444L));
            expectedDocument.set("ft_end_timestamp", new LongNode(1212133455L));

            expectedDocument.set("csac_table", new TextNode("schema.kpi_simple_ssnssai_15"));
            expectedDocument.set("csac_column", new TextNode("csac_0fcf6508_67cc_4969_1f2f_566c106e38b0"));
            expectedDocument.set(
                    "xv_b52e5f0454_8183b74442_eJxljssKwjAQRX03lzLpf4K5YA10101C03M03xGSUQDoJ7UQs4r027CAEfuwvn3Mu9AZkJYQPdKEY01tMezPM7QgvNLCmaVb01o5hBYyea6Z102RqnUI0102LQvJmTcRmK8ch9tnpC4VAvStrCq02jpOS6HVcNhpJZXqBv13Bxc7038Q03Ut01UHoNrvsSm029DuD02q03TSU04",
                    BooleanNode.TRUE);

            Assertions.assertEquals(expectedDocument ,documents.get(0), "Testing Expected Record with info field type Time");
        } catch (Exception e) {
                Assertions.fail(e);
        }
    }

    @Test
    public void recordToDocuments_infoField_string() throws IOException {
        var schema = new Schema.Parser().parse(new File("./src/test/resources/avro-schemas/schema4.avsc"));
        var record = new GenericRecordBuilder(schema)
                .set("SNSSAI", "1:1")
                .set("NF", "AMF_BC")
                .set("Collection", "SITE:BC")
                .set("csac_0fcf6508_67cc_4969_1f2f_566c106e38b0", 10.0)
                .set("csac_9a6ec349_5637_4c92_8bfd_a55630f442d5", 99)
                .set("aggregation_begin_time", "1212133444")
                .set("aggregation_end_time", "1212133455")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        var openApiIndexer = mapper.readValue(
                new File("./src/test/resources/json-files/indexer-spec-4.json"),
                EricOssAssuranceIndexerIndexer.class);

        try {
            var writer = new Writer(openApiIndexer, 0);
            System.out.printf("Writer spec: '%s'%n", writer);
            var documents = writer.recordToDocuments(record);
            var expectedDocument = objectMapper.createObjectNode();

            expectedDocument.set("c_SNSSAI", new TextNode("1:1"));
            expectedDocument.set("c_NF", new TextNode("AMF_BC"));
            expectedDocument.set("c_SITE", new TextNode("BC"));
            expectedDocument.set("doc_id", new TextNode("AMFMeanRegNbr__NF_AMF_BC__SITE_BC__SNSSAI_1:1"));

            expectedDocument.set("full_context", new TextNode("NF_SITE_SNSSAI"));

            var arrayNode = objectMapper.createArrayNode();
            arrayNode.add("NF").add("SITE").add("SNSSAI");
            expectedDocument.set("context", arrayNode);

            expectedDocument.set(
                    "xc_eJyrVspLzE1VslLyc4sP9gxxjQ022Cw529FTSUUrOzytJrShRsoquRqgBiqdkFhfkJFb6gYXySnNygEL5yaW5qXklEDGl5HiIwtTi5KLMgpLM02DyQIMQ4t8zUnBQFPzcFZNlaHbgVIDcQaQlMKW5rnPNzclKTQTI4rYP5ligL4YpxWwlRg2pdbC01Af03N6ig0404",
                    BooleanNode.TRUE);

            expectedDocument.set("value_context", new TextNode("NF_SITE_SNSSAI_AMFMeanRegNbr"));
            expectedDocument.set("value_name", new TextNode("AMFMeanRegNbr"));
            expectedDocument.set("vd_NF_SITE_SNSSAI_AMFMeanRegNbr",new DoubleNode(10.0));
            expectedDocument.set("fk_begin_timestamp", new TextNode("1212133444"));
            expectedDocument.set("fk_end_timestamp", new TextNode("1212133455"));
            expectedDocument.set("csac_table", new TextNode("kpi_simple_ssnssai_15"));
            expectedDocument.set("csac_column", new TextNode("csac_0fcf6508_67cc_4969_1f2f_566c106e38b0"));
            expectedDocument.set(
                    "xv_b52e5f0454_8183b74442_eJxljssKwjAQRX03lzLpf4K5YA10101C03M03xGSUQDoJ7UQs4r027CAEfuwvn3Mu9AZkJYQPdKEY01tMezPM7QgvNLCmaVb01o5hBYyea6Z102RqnUI0102LQvJmTcRmK8ch9tnpC4VAvStrCq02jpOS6HVcNhpJZXqBv13Bxc7038Q03Ut01UHoNrvsSm029DuD02q03TSU04",
                    BooleanNode.TRUE);

            Assertions.assertEquals(expectedDocument ,
                    documents.get(0), "Testing expected record with info field type String");
            //To Do: test this when code for generating id is in
            //Assertions.assertEquals(expectedID, "AMFMeanRegNbr__NF_AMF_BC__SITE_BC__SNSSAI_1:1", "Testing expected id");
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }
    @Test
    public void testRecordToDocumentFormat() throws IOException {
        var schema1 = new Schema.Parser().parse(new File("./src/test/resources/avro-schemas/schema1.avsc"));
        var record = new GenericRecordBuilder(schema1)
                .set("c1", "context 1...")
                .set("c2", "XXX:generated")
                .set("rvi", 10)
                .set("rvf", 99.2)
                .set("ri1", "info 1: ?")
                .set("ri2", "info 2: ?")
                .build();

        ObjectMapper mapper = new ObjectMapper();
        var openApiIndexer = mapper.readValue(
                new File("./src/test/resources/json-files/indexer.json"),
                EricOssAssuranceIndexerIndexer.class);

        try {
            var writer = new Writer(openApiIndexer, 0);
            System.out.printf("Writer spec: '%s'%n",writer);
            var documents = writer.recordToDocuments(record);
            System.out.println(documents);
            // Assertions.assertTrue(result.getResponse().getContentAsString().contains("jvm_threads_states_threads"));
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @SneakyThrows
    @Test
    public void xContextEncodeDecode_test() {
        var objectMapper = new ObjectMapper();
        String contextFieldString = """
                [
                 {"name":"name"},
                 {"name":"long-name-1","displayName":"a display name 1","nameType":"colonSeparated","recordName":"RECORD NAME 1","description":"a lon2g lzong vlozng jlofgngd long 2lo4n5g lo78ng long lioewrioerioperkodrvjldv vsdgbng long long 15 loxxxng long description 1"},
                 {"name":"long-name-2","displayName":"a display name 2","nameType":"colonSeparated","recordName":"RECORD NAME 2","description":"a long long long lozzng locng vlong loncg long cblong long lxcong lonxxg losng long long lonhczg lvong long description 2"},
                 {"name":"long-name-3","displayName":"a display name 3","nameType":"colonSeparated","recordName":"RECORD NAME 3","description":"a ldlfkhjdlfk dflkdfj ewirir dkjdkdkdklong lo23ng lyyyyyyyong long lonyyg long lonyg lyong long loyng description 3"},
                 {"name":"long-name-4","displayName":"a display name 4","nameType":"colonSeparated","recordName":"RECORD NAME 4","description":"a long long long long long long lo3444 long long etDescription(long long long long long longo long long description 4"},
                 {"name":"long-name-5","displayName":"a display name 2","nameType":"colonSeparated","recordName":"RECORD NAME 2","description":"a lotng ltong longt 049458785 23949494 kkkkk23456 skfhfl;s long lo5ng lon5g long lsong long long lsong looong long l555osng description 5"},
                 {"name":"long-name-6","displayName":"a display name 3","nameType":"colonSeparated","recordName":"RECORD NAME 3","description":"a lo999ng lonng looong long sl9g lon999g long l9ongs long long long lo9ng long l5onxg long long xlong long description 6"},
                 {"name":"long-name-7","displayName":"a display name 4","nameType":"colonSeparated","recordName":"RECORD NAME 4","description":"a long l123ong l3333ong long long long longoo lo1234g l3g londddg long l4ong lonxo__ew 3l5o4ndg lgong lonfg l-o=ng ewrf gbfb  flrr;'gb odgbgbf;gblr;r;r;r;r; clong description 7"}
                ]
                """;

        // var xContext = new EricOssAssuranceIndexerFullContext("name_longName1_longName2_longName3_longName4_longName5_longName6_longName7", new ArrayList <>());
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        JavaType myType = typeFactory.constructCollectionType(ArrayList.class, JsonNode.class);
        List<JsonNode> contextFieldStrings = objectMapper.readValue(contextFieldString,myType);
        var contextItems = new ArrayList<EricOssAssuranceIndexerContextFieldSpec>();
        var fullName = new ArrayList<String>();
        for( var field: contextFieldStrings ) {
            var name = field.get("name").asText();
            var contextItem = new EricOssAssuranceIndexerContextFieldSpec(name,"c_"+name);
            contextItem.setDescription(field.has("description") ? field.get("description").asText() : null);
            contextItem.setDisplayName(field.has("displayName") ? field.get("displayName").asText() : null);
            contextItems.add(contextItem);
            fullName.add(name);
        }
        var xContext = new EricOssAssuranceIndexerFullContext(String.join("_", fullName), contextItems);

        // var encoded = Writer.encode(xContext);
        var encoded = Serializer.encode(xContext);
//        Assertions.assertNotEquals('=', encoded.charAt(encoded.length() - 1),
//                "encoded string should not end with '='");

        // var decoded = Writer.decode(encoded, EricOssAssuranceIndexerFullContext.class);
        var decoded = Serializer.decode(encoded, EricOssAssuranceIndexerFullContext.class);
        Assertions.assertEquals(xContext, decoded);

        System.out.printf("encoded: char[%d]: 'xc_%s'%n", encoded.length()+3, encoded);
        System.out.printf("decoded: '%s'%n", decoded);

        Assertions.assertTrue(("xc_"+encoded).length() < 2048,
                "encoded string should be shorter than 2048 characters - OpenSearch field name max length");

        var xValue = new EricOssAssuranceIndexerValueDocumentSpec("value-name", "value-display-name", "value-description");

        var encoded2 = Serializer.encode(xValue);

        Assertions.assertTrue(("xx_"+encoded2).length() < 2048,
                "encoded string should be shorter than 2048 characters - OpenSearch field name max length");
        var decoded2 = Serializer.decode(encoded2, EricOssAssuranceIndexerValueDocumentSpec.class);
        Assertions.assertEquals(xValue, decoded2);

    }

    @Test
    public void validateXField_test() {
        // a long string of 2373 chars producing 2087 char encoded string
        var longString = """
        zbnhbsoksjdawtwsvayzkrpazqortvcelxaesfigmjwtbhegrjxxskzdadfewebudkkndwfgiokaioddghdrsongqfwnvdfrvgvrfjmiqceqzchv
        vcrkzcpxdskplwkwdpqlsurbqtjsqnwuzxrxhpzhlghczffprneexgxdjrqziqzrvjgrqsgwkbkvzdmsbmzozrkhumhqoriqdzubofqjaimrtfze
        jetdmamgjunzxebdwhcqpfpycpmlchmuqmgsulghoperrqvqxajxfpkuwqdgwrzixzsibzjqbjhbxwiaeivkstgshifwhfeuffbdphdmqynwpfrt
        tqxqolgfhwnoiyewdqfxauryyafxginrnouqbbyphijecvnugunmbbukxucmlzyyvyzrxeypakkeyrniehonmhhfwqfmxezcmlcgjczxsimiiqpd
        nrdsecrnrltfvrdkccgcpvxhgdwjklcdlypcrurgfurcdnjtohftblgswesbkkhnhxkkqlrobrdzjychtxggnjwxyuwisfqpgeudzsqkkjhjrmqv
        ptvaubxzbjszsledvxwmyoyigkndyotajfzchdvninjdkxkxwmwrmfarxcqigwpebhonmmcqwqgotkelmsrmxtnurbpjuivcgpxbprqqvotstxzd
        uyoezmptsbekqlxhfxveutxohndglpklbtjluwkdkfewbitivuqdkaygsmwyfpylesntmnxdofvlasqmnmqojozohrzkmpzqdiuzqqzvjcdewohr
        rfryfixubbtroxwxbozbjmuydzyvhjgrwkeuzbkmurvjqvsygbwimhdorezmrlmlapfqmhxhinhtgxwrpipluhmfnrakjrgtxdxqzikgejemxzbz
        fleivcuhwouipjhexlbduwngigdbscnxhpgvtfcqiclojibgdncpnteukoneutqujlaqpyukgqzxqhmvoneydrbwugkomipxmcswphrybchhxprs
        nrpuutsflihxvbhdzzykuqsobozzychqonpigopvmqogsxvbjhpxdysqibbboeplkgxbifnrltqhpvrilltsnupvxtdxdhjiflggvcmpnhbhgltz
        oyzddlfbgjpdxnnscyobjcjpcicabncklhbghbruxpksrmjawermxdjgqggabhljbnikgmgrnduiwlbtzhffauentplazmjenmgpkvhjatgcwvyf
        zdurajwqnjbwmzhguxemrtutzoffesrtvyhspxrhzomjzokzbdeefrpbktgklthasqrdovifbuvpjxvuttvxfkoqplfaikkwcgtrwohxphvkcufe
        sxaehyhunrbvshdzynkfalrkpjddpslmkelbqottaxkverpjfwzdlaxtamrrvqprtqyuwencrmzhvrkhsqvbjmdihkmsdyffvdlkwxwmqrojcbrn
        xikxwionqwndmuvkewxbavndzqamcyaqjlgvegcyhbcwnoqbacpdlfkjawniceathjmwdpigaxcxmopcchpzxropzrkygksjaljoamtccyvnyhoq
        hkpoowscbhsdldrpjphvstbjeeyrtlthalzqegovpqnbsdumruwhsvznumsxqmrulbzyohunkcdeqdcticjqazdrwykhsoyodaiyeqzxqssfzpxr
        ovjepqezksargjpyaavndtzppicsvabdnwpcrhimwvgewkpjmprbgumpaqxpsevglmdjvfxbcpsyxqnxttnsopvpllwzdtjvdfvgsymogbmexeoe
        lbacycziajmjfmgjwawbqmuklrqhqewcpxqybwwaisscuzoffqubwdjeuhhnbizluerimyajqneuohpxlkmqrmdsvnwsrgkhiupvbyhpiylipmuy
        yatsylleazmdhfytdbkdtiyusxqzzqspcvvodxarpiuwrirzmvpakzxmsxhwawpbdhlpaiqyryritpmqdxzdyeknjdjhdstynkrpfsrekkuwyoye
        wcfntltcbwszqyrwhzrfrtclwvuavadwwdbbmcifgblzgewjdoeokmpcnsbmwdaouziktrygodbchipbaviinrajnvvuqovgxjxzovnpuhioqscd
        smyrocdnylhbcaxkiqwekjodkqptmzsvfbwzcnlzmzywedqvurgdgqpgkwopurzmmyqijfkuznnpuksrokokgdfmzhyqnswsldjmmuyixxvaripv
        idaczdtpysswsmnqioxfovzihazvaeiexnrdubakcveozsctcfemlnmcnevwvomrrmcwpnkgxayjhgvsgjzkgtqxdxsezoihmoztqefolalsvdon
        """;
        var xField = Serializer.encode(longString);
        Assertions.assertEquals(longString, Serializer.decode(xField, String.class),
                "should be able to decode encoded string");

        Assertions.assertThrows(IllegalArgumentException.class, () ->
                        Writer.validateXField(xField, "String", longString),
                "should throw IllegalArgumentException when xField is longer than 2048 characters");



    }

    @SneakyThrows
    @Test
    public void getField_test() {
        var record = Mockito.mock(GenericRecord.class);
        var e = Mockito.mock(AvroRuntimeException.class);
        Mockito.when(e.getMessage()).thenReturn("A message ...");
        var fieldName = "field-name";
        Mockito.when(record.get(fieldName)).thenThrow(e);
        Assertions.assertNull(Writer.getField(record, fieldName),
                "should return null when AvroRuntimeException is thrown");
    }
}
