package io.choerodon.agile.api.controller.v1

import com.alibaba.fastjson.JSONObject
import io.choerodon.agile.AgileTestConfiguration
import io.choerodon.agile.api.dto.*
import io.choerodon.agile.infra.dataobject.ProductVersionDO
import io.choerodon.agile.infra.dataobject.VersionIssueRelDO
import io.choerodon.agile.infra.mapper.IssueMapper
import io.choerodon.agile.infra.mapper.ProductVersionMapper
import io.choerodon.agile.infra.mapper.VersionIssueRelMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.test.context.ActiveProfiles
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise

import java.text.SimpleDateFormat

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(AgileTestConfiguration)
@ActiveProfiles("test")
@Stepwise
class ProductVersionControllerSpec extends Specification {

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    ProductVersionMapper productVersionMapper

    @Autowired
    VersionIssueRelMapper versionIssueRelMapper

    @Autowired
    IssueMapper issueMapper

    @Shared
    def versionName = "version_test" + System.currentTimeMillis()

    @Shared
    def versionName2 = "version_test2" + System.currentTimeMillis()

    @Shared
    def versionName3 = "version_test3" + System.currentTimeMillis()

    @Shared
    def versionMergeName = "version_merge" + System.currentTimeMillis()

    @Shared
    def projectId = 1L

    @Shared
    ProductVersionDO result

    @Shared
    ProductVersionDO result2

    @Shared
    ProductVersionDO result3

    private Date StringToDate(String str) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.parse(str)
    }

    def 'createVersion'() {
        given:
        ProductVersionCreateDTO productVersionCreateDTO = new ProductVersionCreateDTO()
        productVersionCreateDTO.projectId = projectId
        productVersionCreateDTO.name = name
        productVersionCreateDTO.startDate = startDate
        productVersionCreateDTO.releaseDate = releaseDate

        when:
        HttpEntity<ProductVersionCreateDTO> productVersionCreateDTOHttpEntity = new HttpEntity<>(productVersionCreateDTO);
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version",
                HttpMethod.POST,
                productVersionCreateDTOHttpEntity,
                ProductVersionDetailDTO.class,
                projectId)

        then:
        entity.statusCode.is2xxSuccessful()

        expect:
        entity.body.name == resultName

        where:
        projectId | startDate                           | releaseDate                         | name             | resultName
        1L        | null                                | null                                | versionName      | versionName
        1L        | null                                | null                                | null             | null
        1L        | StringToDate("2018-08-22 00:00:00") | StringToDate("2018-08-21 00:00:00") | versionName2     | null
        1L        | null                                | null                                | versionName      | null
        1L        | null                                | null                                | versionName3     | versionName3
        1L        | null                                | null                                | versionMergeName | versionMergeName
    }

    def 'select product version'() {
        given:
        ProductVersionDO productVersionDO = new ProductVersionDO()
        productVersionDO.name = versionName
        productVersionDO.projectId = projectId
        result = productVersionMapper.selectOne(productVersionDO)
        productVersionDO.name = versionName3
        result2 = productVersionMapper.selectOne(productVersionDO)
        result2.statusCode = "released"
        productVersionMapper.updateByPrimaryKey(result2)
        productVersionDO.name = versionMergeName
        result3 = productVersionMapper.selectOne(productVersionDO)

        // 初始化version、issue关联关系
        VersionIssueRelDO versionIssueRelDO = new VersionIssueRelDO()
        versionIssueRelDO.projectId = projectId
        versionIssueRelDO.issueId = 2L
        versionIssueRelDO.versionId = result.versionId
        versionIssueRelDO.relationType = "fix"
        versionIssueRelMapper.insert(versionIssueRelDO)
    }

    def 'updateVersion'() {
        given:
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("versionId", result.getVersionId())
        jsonObject.put("description", versionName)
        jsonObject.put("name", result.getName())
        jsonObject.put("projectId", result.getProjectId())

        when:
        jsonObject.put("objectVersionNumber", result.objectVersionNumber)
        HttpEntity<JSONObject> jsonObjectHttpEntity = new HttpEntity<>(jsonObject)
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}",
                HttpMethod.PUT,
                jsonObjectHttpEntity,
                ProductVersionDetailDTO.class,
                projectId,
                result.getVersionId())

        jsonObject.put("objectVersionNumber", 0L)
        HttpEntity<JSONObject> changeObject = new HttpEntity<>(jsonObject)
        def entityException = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}",
                HttpMethod.PUT,
                changeObject,
                String.class,
                projectId,
                result.getVersionId())

        then:
        entity.statusCode.is2xxSuccessful()
        entity.body.description == versionName
        JSONObject exceptionInfo = JSONObject.parse(entityException.body)
        exceptionInfo.get("failed").toString() == "true"
        exceptionInfo.get("code").toString() == "error.version.update"

    }

    def 'checkName'() {
        when:
        def entityTrue = restTemplate.exchange("/v1/projects/{project_id}/product_version/{name}/check",
                HttpMethod.GET,
                new HttpEntity<>(),
                Boolean.class,
                projectId,
                versionName)
        def entityFalse = restTemplate.exchange("/v1/projects/{project_id}/product_version/{name}/check",
                HttpMethod.GET,
                new HttpEntity<>(),
                Boolean.class,
                projectId,
                versionName2)

        then:
        entityTrue.statusCode.is2xxSuccessful()
        entityTrue.body == true

        entityFalse.statusCode.is2xxSuccessful()
        entityFalse.body == false

    }

    def 'queryVersionByProjectId'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version",
                HttpMethod.GET,
                new HttpEntity<>(),
                List.class,
                projectId)
        then:
        entity.statusCode.is2xxSuccessful()
        entity.body.size() > 0
    }

    def 'queryVersionByVersionId'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/detail",
                HttpMethod.GET,
                new HttpEntity<>(),
                ProductVersionDetailDTO.class,
                projectId,
                result.versionId)
        def entityNull = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/detail",
                HttpMethod.GET,
                new HttpEntity<>(),
                ProductVersionDetailDTO.class,
                projectId,
                0L)

        then:
        entity.statusCode.is2xxSuccessful()
        entity.body.name == versionName
        entityNull.statusCode.is2xxSuccessful()
    }

    def 'releaseVersion'() {
        given:
        ProductVersionReleaseDTO productVersionReleaseDTO = new ProductVersionReleaseDTO()
        productVersionReleaseDTO.projectId = projectId
        productVersionReleaseDTO.releaseDate = new Date()
        productVersionReleaseDTO.versionId = result.versionId
        productVersionReleaseDTO.targetVersionId = result2.versionId

        when:
        HttpEntity<ProductVersionReleaseDTO> httpEntity = new HttpEntity<>(productVersionReleaseDTO)
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/release",
                HttpMethod.POST,
                httpEntity,
                ProductVersionDetailDTO.class,
                projectId)
        and:
        // 设置查询versionIssueRel条件
        VersionIssueRelDO search = new VersionIssueRelDO()
        search.versionId = result.versionId
        List<VersionIssueRelDO> relsOrigin = versionIssueRelMapper.select(search)
        search.versionId = result2.versionId
        List<VersionIssueRelDO> relsNew = versionIssueRelMapper.select(search)


        then:
        entity.statusCode.is2xxSuccessful()
        entity.body.statusCode == "released"
        relsOrigin.size() == 1
        relsNew.size() == 1
        relsNew.get(0).issueId == 2L
    }

    def 'releaseVersion unSuccess'() {
        given:
        ProductVersionReleaseDTO productVersionReleaseDTO = new ProductVersionReleaseDTO()
        productVersionReleaseDTO.projectId = projectId
        productVersionReleaseDTO.releaseDate = new Date()
        productVersionReleaseDTO.versionId = result2.versionId

        when:
        HttpEntity<ProductVersionReleaseDTO> httpEntity = new HttpEntity<>(productVersionReleaseDTO)
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/release",
                HttpMethod.POST,
                httpEntity,
                String.class,
                projectId)


        then:
        entity.statusCode.is2xxSuccessful()
        JSONObject exceptionInfo = JSONObject.parse(entity.body)
        exceptionInfo.get("failed").toString() == "true"
    }

    def 'revokeReleaseVersion'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/revoke_release",
                HttpMethod.POST,
                new HttpEntity<>(),
                ProductVersionDetailDTO.class,
                projectId,
                result.versionId)
        then:
        entity.statusCode.is2xxSuccessful()
        entity.body.statusCode == "version_planning"
    }

    def 'revokeReleaseVersion unSuccess'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/revoke_release",
                HttpMethod.POST,
                new HttpEntity<>(),
                String.class,
                projectId,
                result.versionId)
        then:
        entity.statusCode.is2xxSuccessful()
        JSONObject exceptionInfo = JSONObject.parse(entity.body)
        exceptionInfo.get("failed").toString() == "true"
        exceptionInfo.get("code").toString() == "error.productVersion.revokeRelease"
    }

    def 'archivedVersion'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/archived",
                HttpMethod.POST,
                new HttpEntity<>(),
                ProductVersionDetailDTO.class,
                projectId,
                result.versionId)
        then:
        entity.statusCode.is2xxSuccessful()
        entity.body.statusCode == "archived"
    }

    def 'archivedVersion unSuccess'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/archived",
                HttpMethod.POST,
                new HttpEntity<>(),
                String.class,
                projectId,
                result.versionId)
        then:
        entity.statusCode.is2xxSuccessful()
        JSONObject exceptionInfo = JSONObject.parse(entity.body)
        exceptionInfo.get("failed").toString() == "true"
        exceptionInfo.get("code").toString() == "error.productVersion.archived"
    }

    def 'revokeArchivedVersion'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/revoke_archived",
                HttpMethod.POST,
                new HttpEntity<>(),
                ProductVersionDetailDTO.class,
                projectId,
                result.versionId)
        then:
        entity.statusCode.is2xxSuccessful()
        entity.body.statusCode == "version_planning"
    }

    def 'mergeVersion'() {
        given:
        ProductVersionMergeDTO productVersionMergeDTO = new ProductVersionMergeDTO()
        List<Long> sourceVersionIds = new ArrayList<>()
        sourceVersionIds.add(result2.versionId)
        productVersionMergeDTO.sourceVersionIds = sourceVersionIds
        productVersionMergeDTO.targetVersionId = result3.versionId

        when:
        HttpEntity<ProductVersionMergeDTO> productVersionMergeDTOHttpEntity = new HttpEntity<>(productVersionMergeDTO)
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/merge",
                HttpMethod.POST,
                productVersionMergeDTOHttpEntity,
                Boolean.class,
                projectId)

        and:
        // 设置查询versionIssueRel条件
        VersionIssueRelDO search = new VersionIssueRelDO()
        search.versionId = result2.versionId
        List<VersionIssueRelDO> relsOrigin = versionIssueRelMapper.select(search)
        search.versionId = result3.versionId
        List<VersionIssueRelDO> relsNew = versionIssueRelMapper.select(search)


        then:
        entity.statusCode.is2xxSuccessful()
        relsOrigin.size() == 0
        relsNew.size() == 1
        relsNew.get(0).issueId == 2L

    }

    def 'queryVersionStatisticsByVersionId'() {
        given:
        productVersionMapper.selectAll()
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}",
                HttpMethod.GET,
                null,
                ProductVersionStatisticsDTO.class,
                projectId,
                versionId)

        then:
        entity.statusCode.is2xxSuccessful()

        and:
        ProductVersionStatisticsDTO productVersionStatisticsDTO =entity.body

        expect: "设置期望值"
        productVersionStatisticsDTO.projectId==projectId
        productVersionStatisticsDTO.statusCode==statusCode

        where: '比较期望值'
        versionId | statusCode
        1L        | "version_planning"
        2L        | "version_planning"

    }

    def 'queryReleaseMessageByVersionId'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/plan_names",
                HttpMethod.GET,
                null,
                VersionMessageDTO.class,
                projectId,
                versionId)

        then:
        entity.statusCode.is2xxSuccessful()

        and:
        VersionMessageDTO result = entity.body

        expect: "设置期望值"
        result.fixIssueCount==fixIssueCount
        result.versionNames.size()==expectCount

        where: '对比结果'
        versionId || fixIssueCount | expectCount
        1L        || 1             | 2
        2L        || 1             | 2
    }

    def 'queryDeleteMessageByVersionId'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/names",
                HttpMethod.GET,
                null,
                VersionMessageDTO.class,
                projectId,
                versionId)

        then:
        entity.statusCode.is2xxSuccessful()

        and:
        VersionMessageDTO result = entity.body

        expect: "设置期望值"
        result.fixIssueCount==fixIssueCount
        result.versionNames.size()==expectCount

        where: '对比结果'
        versionId || fixIssueCount | expectCount
        1L        || 1             | 2
        2L        || 1             | 2
    }

    def 'queryNameByOptions'() {
        given:
        List<String> list=new ArrayList<>()
        list.add("version_planning")
        HttpEntity<List<String>> requestEntity = new HttpEntity<>(list,null)

        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/names",
                HttpMethod.POST,
                requestEntity,
                List,
                projectId)

        then:
        entity.statusCode.is2xxSuccessful()

        and:
        List<ProductVersionNameDTO> result = entity.body

        expect: "设置期望值"
        result.size()==expectCount
        result.get(0).statusCode==statusCode

        where: '对比结果'
        versionId || expectCount | statusCode
        1L        || 3           | "version_planning"
        2L        || 3           | "version_planning"
    }

    def 'listByProjectId'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/versions",
                HttpMethod.GET,
                null,
                List,
                projectIds)

        then:
        entity.statusCode.is2xxSuccessful()

        and:
        List<ProductVersionDTO> result = entity.body

        expect: "设置期望值"
        result.size()==expectCount
        try{
            result.get(0).statusCode==statusCode
        }catch (Exception e){
            statusCode=Exception
        }


        where: '对比结果'
        projectIds || expectCount | statusCode
        1L         || 3           | "version_planning"
        2L         || 0           | Exception
    }

    def 'listIds'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/ids",
                HttpMethod.GET,
                null,
                List,
                projectIds)

        then:
        entity.statusCode.is2xxSuccessful()

        and:
        List<Long> result = entity.body

        expect: "设置期望值"
        result.size()==expectCount

        where: '对比结果'
        projectIds || expectCount
        1L         || 3
        2L         || 3
    }

    def 'dragVersion'() {
        given:
        VersionSequenceDTO versionSequenceDTO=new VersionSequenceDTO()
        versionSequenceDTO.versionId=2L
        versionSequenceDTO.beforeSequence=4
        HttpEntity<VersionSequenceDTO> requestEntity = new HttpEntity<>(versionSequenceDTO,null)

        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/drag",
                HttpMethod.PUT,
                requestEntity,
                ProductVersionPageDTO,
                projectId)

        then:
        entity.statusCode.is2xxSuccessful()

        and:
        ProductVersionPageDTO result = entity.body

        expect: "设置期望值"
        result.statusCode=="version_planning"
        result.sequence==2
    }

    def 'queryByCategoryCode'() {
        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/issue_count",
                HttpMethod.GET,
                null,
                VersionIssueCountDTO,
                projectId,
                versionIds)

        then:
        entity.statusCode.is2xxSuccessful()

        and:
        VersionIssueCountDTO result = entity.body

        expect: "设置期望值"
        result.metaPropertyValues.size()==expectCount
        result.doingIssueCount=doingIssueCount

        where:
        versionIds | expectCount | doingIssueCount
        1L         | 1L          | 2L
        2L         | 1L          | 2L
    }
//    def 'queryByVersionIdAndStatusCode'() {
//        when:
//        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/issues?statusCode={statusCode}",
//                HttpMethod.GET,
//                null,
//                List,
//                projectId,
//                versionId,
//                statusCode)
//
//        and:
//        entity.statusCode.is2xxSuccessful()
//
//        then:
//        List<IssueListDTO> result = entity.body
//
//        expect: "设置期望值"
//        result.size() == 10
//        result.get(0).projectId == projectId
//        result.get(0).issueId == issueId
//
//        where: '对比结果'
//        versionId | statusCode         || issueId
//        1L        | 'version_planning' || 1L
//    }

//    def 'queryByVersionIdAndStatusCode'() {
//        when:
//        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}/issues",
//                HttpMethod.GET,
//                null,
//                List,
//                projectId,
//                versionId,
//                statusCode)
//
//        then:
//        entity.statusCode.is2xxSuccessful()
//
//        and:
//        List<IssueListDTO> result = entity.body
//
//        expect: "设置期望值"
//        result.size() == 10
//        result.get(0).projectId == projectId
//        result.get(0).issueId == issueId
//
//        where: '对比结果'
//        versionId | statusCode || issueId
//        1L        | 'released' || 1L
//    }

    def 'deleteVersion'() {
        given:
        ProductVersionDO productVersionDO = new ProductVersionDO()
        productVersionDO.name = versionMergeName
        productVersionDO.projectId = projectId
        VersionIssueRelDO versionIssueRelDO = new VersionIssueRelDO()
        versionIssueRelDO.projectId = projectId
        versionIssueRelDO.versionId = result.versionId

        when:
        def entity = restTemplate.exchange("/v1/projects/{project_id}/product_version/{versionId}",
                HttpMethod.DELETE,
                new HttpEntity<>(),
                Boolean.class,
                projectId,
                result3.getVersionId())
        versionIssueRelMapper.delete(versionIssueRelDO)

        then:
        entity.statusCode.is2xxSuccessful()
        ProductVersionDO deleteDO = productVersionMapper.selectOne(productVersionDO)
        deleteDO == null
        versionIssueRelMapper.selectAll().size() == 1
    }


}
