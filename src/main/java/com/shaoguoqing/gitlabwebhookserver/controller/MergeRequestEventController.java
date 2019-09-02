package com.shaoguoqing.gitlabwebhookserver.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @Description: The MergeRequestEventController
 * @Author: shaoguoqing
 * @Date: 2019/8/31 02:08
 */
@RestController
@RequestMapping("/event/mergeRequest")
public class MergeRequestEventController {

    @Value("${gitlab.api.token}")
    private String gitlabApiToken;

    @Value("${gitlab.domain}")
    private String gitlabDomain;

    @PostMapping("")
    public void triggerEvent(@RequestBody String json) {

        System.out.println("receive event: " + json);

        JSONObject eventObject = JSON.parseObject(json);

        String eventType = eventObject.getString("event_type");

        if (!Objects.equals(eventType, "merge_request")) {
            System.out.println("不支持的eventType:" + eventType);
            return;
        }

        JSONObject projectObject = eventObject.getJSONObject("project");
        JSONObject attributeObject = eventObject.getJSONObject("object_attributes");

        Long projectId = projectObject.getLong("id");
        String projectName = projectObject.getString("name");

        Long mergeRequestId = attributeObject.getLong("iid");
        String mergeTitle = attributeObject.getString("title");
        String mergeDescription = attributeObject.getString("description");
        String mergeState = attributeObject.getString("state");
        String mergeCommitSha = attributeObject.getString("merge_commit_sha");
        String targetBranch = attributeObject.getString("target_branch");

        JSONObject lastCommitObject = attributeObject.getJSONObject("last_commit");
        String lastCommitSha = lastCommitObject.getString("id");

        JSONObject lastCommitAuthorObject = lastCommitObject.getJSONObject("author");
        String lastCommitAuthor = lastCommitAuthorObject.getString("name");

        if (!Objects.equals(mergeState, "merged")) {
            System.out.println("不支持的mergeState:" + mergeState);
            return;
        }

        if (!Objects.equals(targetBranch, "master")) {
            System.out.println("不支持的targetBranch:" + targetBranch);
            return;
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        String tag = projectName + "_v_" + simpleDateFormat.format(new Date());

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("PRIVATE-TOKEN", gitlabApiToken);

        String tagUrl = gitlabDomain + "/api/v4/projects/" + projectId + "/repository/tags";

        Map<String, String> tagParam = new HashMap<>();
        tagParam.put("tag_name", tag);
        tagParam.put("ref", mergeCommitSha);
        tagParam.put("message", mergeTitle);

        HttpEntity<String> tagEntity = new HttpEntity<>(JSON.toJSONString(tagParam), headers);

        ResponseEntity<String> tagResult = restTemplate.exchange(tagUrl, HttpMethod.POST, tagEntity, String.class);
        System.out.println("tagResult:" + tagResult.getStatusCode());

        String commentUrl = gitlabDomain + "/api/v4/projects/" + projectId + "/merge_requests/" + mergeRequestId + "/notes";

        Map<String, String> commentParam = new HashMap<>();
        commentParam.put("body",
                          "| 项目 | 值 |\n"
                        + "| -------- | -------- |\n"
                        + "| TAG | " + tag + " |\n"
                        + "| PROJECT NAME | " + projectName + " |\n"
                        + "| MERGE TITLE | " + mergeTitle + " |\n"
                        + "| MERGE DESCRIPTION |" + mergeDescription + " |\n"
                        + "| LAST COMMIT AUTHOR | @" + lastCommitAuthor + " |\n"
                        + "| LAST COMMIT SHA | " + lastCommitSha + " |");

        HttpEntity<String> commentEntity = new HttpEntity<>(JSON.toJSONString(commentParam), headers);

        ResponseEntity<String> commentResult = restTemplate.exchange(commentUrl, HttpMethod.POST, commentEntity, String.class);
        System.out.println("commentResult:" + commentResult.getStatusCode());

        System.out.println("merge request event处理结束");
    }

}
