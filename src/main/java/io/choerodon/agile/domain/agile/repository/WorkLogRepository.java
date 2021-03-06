package io.choerodon.agile.domain.agile.repository;

import io.choerodon.agile.domain.agile.entity.WorkLogE;

/**
 * Created by HuangFuqiang@choerodon.io on 2018/5/18.
 * Email: fuqianghuang01@gmail.com
 */
public interface WorkLogRepository {

    WorkLogE create(WorkLogE workLogE);

    WorkLogE update(WorkLogE workLogE);

    void delete(Long logId);

}
