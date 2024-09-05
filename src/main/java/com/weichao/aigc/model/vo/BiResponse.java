package com.weichao.aigc.model.vo;


import lombok.Data;

/**
 * BI 的返回结果
 */
@Data
public class BiResponse {

    /**
     * 生成的图表
     */
    private String genChart;


    /**
     * 生成的分析结论
     */
    private String genResult;

    /**
     * 图表 ID
     */
    private Long chartId;


}
