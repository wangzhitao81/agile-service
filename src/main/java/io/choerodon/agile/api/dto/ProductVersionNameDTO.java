package io.choerodon.agile.api.dto;

import java.util.Date;

/**
 * Created by jian_zhang02@163.com on 2018/6/4.
 */
public class ProductVersionNameDTO {
    private Long versionId;
    private String name;
    private String statusCode;
    private Date releaseDate;

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }
}
