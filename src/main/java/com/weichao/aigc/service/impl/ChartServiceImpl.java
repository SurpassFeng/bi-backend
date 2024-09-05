package com.weichao.aigc.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.weichao.aigc.constant.CommonConstant;
import com.weichao.aigc.model.dto.chart.ChartQueryRequest;
import com.weichao.aigc.model.entity.Chart;

import com.weichao.aigc.service.ChartService;
import com.weichao.aigc.mapper.ChartMapper;
import com.weichao.aigc.utils.SqlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;


/**
* @author User2
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2024-08-11 23:09:46
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{


    /**
     * 从 SpringBoot 连接池中获取数据库的连接
     */
    @Resource
    private DataSource dataSource;

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String name = chartQueryRequest.getName();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }



    /**
     * 根据 chartId 创建关于csvData的数据表 (分表存储）
     * @param chartId
     * @param csvData
     * @return
     */
    @Override
    public boolean createCsvDataTable(Long chartId, String csvData){
        String tableName = "chart_" + chartId.toString();

        // 解析 csvData 获取列字段名
        String[] rowList = csvData.split("\n");
        // read the first row
        List<String> columnNames = Arrays.asList(rowList[0].split(","));

        // 建表
        StringBuilder createTableQuery = new StringBuilder("CREATE TABLE IF NOT EXISTS " + tableName + " (");
        for (String columnName : columnNames) {
            createTableQuery.append(columnName).append(" VARCHAR(128) NULL, ");
        }
        createTableQuery.setLength(createTableQuery.length() - 2); // 删除最后一个逗号和空格
        createTableQuery.append(") COLLATE = utf8mb4_unicode_ci;");
        try {
            // create table
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            stmt.execute(createTableQuery.toString());
            System.out.println("Table " + tableName + " created successfully.");


            // insert data
            String insertQuery = "INSERT INTO " + tableName + " (" + String.join(",", columnNames)
                    + ") VALUES (" + String.join(",", columnNames.stream().map(col -> "?").toArray(String[]::new)) + ")";
            PreparedStatement pstmt = conn.prepareStatement(insertQuery);
            // read the rest row
            for (int i = 1; i < rowList.length; i++) {
                List<String> rowData = Arrays.asList(rowList[i].split(","));

                for (int j = 0; j < rowData.size(); j++) {
                    pstmt.setString(j + 1, rowData.get(j));
                }
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            System.out.println("Data inserted successfully into table " + tableName);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    /**
     * 图表接口很多用到异常,定义一个工具类
     *
     * @param chartId
     * @param execMessage
     */
    @Override
    public void handleChartUpdateError(long chartId, String execMessage) {
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chartId);
        updateChartResult.setStatus("failed");
        updateChartResult.setExecMessage(execMessage);
        boolean updateResult = this.updateById(updateChartResult);
        if (!updateResult) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }
}




