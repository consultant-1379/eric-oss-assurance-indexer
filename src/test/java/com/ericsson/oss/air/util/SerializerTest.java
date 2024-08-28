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

package com.ericsson.oss.air.util;

import com.ericsson.oss.air.api.generated.model.EricOssAssuranceIndexerValueDocumentSpec;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SerializerTest {

    @Test
    public void testEncode() {
        var xValue = new EricOssAssuranceIndexerValueDocumentSpec("value-name", "value-display-name", "value-description");
        String base62 = Serializer.encode(xValue);
        System.out.println("xValue: " + xValue);
        System.out.println("base62: " + base62);

        var o = Serializer.decode(base62, EricOssAssuranceIndexerValueDocumentSpec.class);
        assertEquals(xValue, o);
    }
}