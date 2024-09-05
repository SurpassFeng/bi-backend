package com.weichao.aigc.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.weichao.aigc.model.dto.chart.ChartQueryRequest;
import com.weichao.aigc.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author weichao
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2024-08-11 23:09:46
*/
public interface ChartService extends IService<Chart> {
    /**
     * 获取查询条件
     *
     * @param chartQueryRequest
     * @return
     */
    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);

    /**
     * 根据 chartId 创建关于csvData的数据表
     * @param chartId
     * @param csvData
     * @return
     */
    boolean createCsvDataTable(Long chartId, String csvData);

    /**
     * 图表接口很多用到异常,定义一个工具类
     *
     * @param chartId
     * @param execMessage
     */
    void handleChartUpdateError(long chartId, String execMessage);
}
