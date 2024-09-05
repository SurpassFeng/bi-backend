package com.weichao.aigc.manager;


import com.weichao.aigc.common.ErrorCode;
import com.weichao.aigc.exception.ThrowUtils;
import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;

import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *  对接 AI 平台
 */
@Component
public class AiManager {

    @Resource
    private SparkClient sparkClient;


    private static final String SYSTEM_PROMPT = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
            "分析需求：\n" +
            "{数据分析的需求或者目标}\n" +
            "原始数据：\n" +
            "{csv格式的原始数据， 用 ,作为分隔符}\n" +
            "请根据这两部分内容，按照以下格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
            "【【【【【\n" +
            "{前端Echarts V5 的option配置对象js代码，合理地将数据进行可视化，不要生成多余的内容，比如注释}\n" +
            "【【【【【\n" +
            "{明确的数据分析结论、越详细越好，不要生产多余的注释}" +
            "这是一个示例的输出模板：\n" +
            "【【【【【\n" +
            "{\n" +
            "\t\"title\": {\n" +
            "        \"text\": \"网站用户增长情况\",\n" +
            "        \"subtext\": ''\n" +
            "\t},\n" +
            "    \"tooltip\": {\n" +
            "        \"trigger\": \"axis\",\n" +
            "        \"axisPointer:\": {\n" +
            "            \"type\": \"shadow\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"legend\":{\n" +
            "        \"data\":[\"用户数\"]\n" +
            "    },\n" +
            "    \"xAxis\": {\n" +
            "    \t\"data\": [\"1\", \"2\", \"3\",\"4\", \"5\", \"6\", \"7\"]\n" +
            "    },\n" +
            "    \"yAxis\": {},\n" +
            "    \"series\": [{\n" +
            "        \"name\": \"用户数\",\n" +
            "        \"type\": \"bar\",\n" +
            "        \"data\": [200, 200, 300, 300, 400, 500, 600]\n" +
            "    }]\n" +
            "}\n" +
            "【【【【【\n" +
            "根据数据分析可得，该网站用户数量逐日增长，时间越长，用户数量增长越多。建议网站加强用户留存，提高用户复购率，例如优化用户界面、增强互动等。";


    public String doChat(String message) {
        // 消息列表，可以在此列表添加历史对话记录
        List<SparkMessage> messages=new ArrayList<>();

        messages.add(SparkMessage.systemContent(SYSTEM_PROMPT));
        messages.add(SparkMessage.userContent(message));
        // 构造请求
        SparkRequest sparkRequest=SparkRequest.builder().messages(messages).
        apiVersion(SparkApiVersion.V3_5).build();



        SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
        ThrowUtils.throwIf(chatResponse == null, ErrorCode.SYSTEM_ERROR, "AI 响应错误");

        return chatResponse.getContent();


    }

}
