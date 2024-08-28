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

import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
@Setter
public class IndexerProcessingTimeTracker {

    private Long aisStartTime = 0L;
    private Long aisFinishTime = 0L;

    /**
     * Get processing time for AIS to complete provisioning of all target services
     *
     * @return time in seconds elapsed for AIS processing
     */
    public Double getAisProcessingTime() {
        if (this.aisFinishTime >= this.aisStartTime) {
            return (double) (this.aisFinishTime - this.aisStartTime) / 1000;
        }
        return 0.0;
    }
    public void setAisProcessingStartTime(Long t) {
        this.aisStartTime = t;
    }
    public void setAisProcessingFinishTime(Long t) {
        this.aisFinishTime = t;
    }
}