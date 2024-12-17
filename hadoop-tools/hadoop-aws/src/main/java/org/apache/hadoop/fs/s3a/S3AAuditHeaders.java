package org.apache.hadoop.fs.s3a;

import org.apache.hadoop.fs.audit.AuditConstants;
import org.apache.hadoop.fs.store.audit.HttpReferrerAuditHeader;
import software.amazon.s3.analyticsaccelerator.request.AuditHeaders;

public class S3AAuditHeaders implements AuditHeaders {


    HttpReferrerAuditHeader referrer;
    public S3AAuditHeaders(HttpReferrerAuditHeader referrer) {
        this.referrer = referrer;
    }
    @Override
    public void setGetRange(String range) {
        referrer.set(AuditConstants.PARAM_RANGE, range);

    }

    @Override
    public String buildReferrerHeader() {
        return referrer.buildHttpReferrer();
    }
}
