package com.weichao.aigc.controller;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;


import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.weichao.aigc.annotation.AuthCheck;
import com.weichao.aigc.bizmq.BiMessageProducer;
import com.weichao.aigc.common.BaseResponse;
import com.weichao.aigc.common.DeleteRequest;
import com.weichao.aigc.common.ErrorCode;
import com.weichao.aigc.common.ResultUtils;
import com.weichao.aigc.constant.FileConstant;
import com.weichao.aigc.constant.UserConstant;
import com.weichao.aigc.exception.BusinessException;
import com.weichao.aigc.exception.ThrowUtils;
import com.weichao.aigc.manager.AiManager;
import com.weichao.aigc.manager.RedisLimiterManager;
import com.weichao.aigc.model.dto.chart.*;
import com.weichao.aigc.model.entity.Chart;
import com.weichao.aigc.model.entity.User;
import com.weichao.aigc.model.vo.BiResponse;
import com.weichao.aigc.service.ChartService;
import com.weichao.aigc.service.UserService;
import com.weichao.aigc.utils.ExcelUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;



/**
 * 图表接口
 *
 * @author weichao
 *  
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest 图表添加请求体
     * @param request 网络请求体
     * @return 操作响应体
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest 图表删除请求体
     * @param request 网络请求体
     * @return 操作响应体
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest 图表更新请求体
     * @return 操作响应体
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);

        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id 图表id
     * @return 操作响应体
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest 图表查询请求体
     * @param request 网络请求体
     * @return 操作响应体
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest 图表查询请求体
     * @param request 网络请求体
     * @return 操作响应体
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
            HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                chartService.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest 图表编辑请求体
     * @param request 网络请求体
     * @return 操作响应体
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);

        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * AI数据分析（同步）
     *
     * @param multipartFile 文件
     * @param genChartByAiRequest 智能分析请求体
     * @param request 网络请求体
     * @return 操作响应体
     */
    @PostMapping("/gen")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile,
                                             GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();

        //校验
        // 如果分析目标为空，抛出异常，并提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        // 如果图像名称为空或过长 > 100，抛出异常，并提示
        ThrowUtils.throwIf(StringUtils.isBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "图像名称为空或过长");

        // 文件校验 region
        // 获取文件信息（大小、文件名）
        long fileSize = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        // 校验文件大小，如果大于1MB, 抛出异常，并提示
        ThrowUtils.throwIf(fileSize > FileConstant.ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");

        //校验文件后缀
        String fileSuffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList(".xlsx", ".xls");
        ThrowUtils.throwIf(validFileSuffixList.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型非法");
        // end 文件校验 region


        // 通过 request 拿到用户的id (需登录才能使用）
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimiter("genChartByAi_" + loginUser.getId());


        // 用户输入 Prompt 的设计
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");

        // 拼接数据
        userInput.append("原始数据：").append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 拿到返回结果
        String result = aiManager.doChat(userInput.toString());

        // 对结果进行拆分，按照5个中括号进行拆分
        String[] splits = result.split("【【【【【");
        // 如果拆分结果小于3，抛出异常，并提示
        ThrowUtils.throwIf(splits.length < 3, ErrorCode.SYSTEM_ERROR, "AI 生成错误");

        String genChart = splits[1].trim();
        String genResult = splits[2].trim();

        // 插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setStatus("succeed");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 原始数据分表存储
        boolean createTableResult = chartService.createCsvDataTable(chart.getId(), csvData);
        ThrowUtils.throwIf(!createTableResult, ErrorCode.SYSTEM_ERROR, "原始数据保存失败");


        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);

    }


    /**
     * AI数据分析（异步）
     *
     * @param multipartFile 文件
     * @param genChartByAiRequest 智能分析请求体
     * @param request 网络请求体
     * @return 操作响应体
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();

        //校验
        // 如果分析目标为空，抛出异常，并提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        // 如果图像名称为空或过长 > 100，抛出异常，并提示
        ThrowUtils.throwIf(StringUtils.isBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "图像名称为空或过长");

        // 文件校验 region
        // 获取文件信息（大小、文件名）
        long fileSize = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        // 校验文件大小，如果大于1MB, 抛出异常，并提示
        ThrowUtils.throwIf(fileSize > FileConstant.ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");

        //校验文件后缀
        String fileSuffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList(".xlsx", ".xls");
        ThrowUtils.throwIf(validFileSuffixList.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型非法");
        // end 文件校验 region


        // 通过 request 拿到用户的id (需登录才能使用）
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimiter("genChartByAi_" + loginUser.getId());


        // 用户输入 Prompt 的设计
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");

        // 拼接数据
        userInput.append("原始数据：").append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 先把图表插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 原始数据分表存储
        boolean createTableResult = chartService.createCsvDataTable(chart.getId(), csvData);
        ThrowUtils.throwIf(!createTableResult, ErrorCode.SYSTEM_ERROR, "原始数据保存失败");


        // 在最终的返回结果前提交一个任务
        // todo 建议处理任务队列满了之后，抛异常的情况（因提交任务报错，前端返回异常）
        CompletableFuture.runAsync(() ->{
            // 先修改图表任务状态为 “执行中”。等执行成功后，修改为 “已完成”、保存执行结果；执行失败后，状态修改为 “失败”，记录任务失败信息。(为了防止同一个任务被多次执行)
            Chart updateChart = new Chart();
            updateChart.setId(chart.getId());

            // 把任务状态改为执行中
            updateChart.setStatus("running");
            boolean b = chartService.updateById(updateChart);
            // 如果提交失败(一般情况下,更新失败可能意味着你的数据库出问题了)
            if (!b) {
                chartService.handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                return;
            }

            // 调用AI, 拿到返回结果
            String result = aiManager.doChat(userInput.toString());

            // 对结果进行拆分，按照5个中括号进行拆分
            String[] splits = result.split("【【【【【");
            // 如果拆分结果小于3，抛出异常，并提示
            if (splits.length < 3) {
                chartService.handleChartUpdateError(chart.getId(), "AI 生成错误");
                return;
            }
            String genChart = splits[1].trim();
            String genResult = splits[2].trim();
            // 调用AI得到结果之后,再更新一次
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chart.getId());
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus("succeed");
            boolean updateResult = chartService.updateById(updateChartResult);
            if (!updateResult) {
                chartService.handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
            }
        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);

    }

    /**
     * AI数据分析（异步MQ）
     *
     * @param multipartFile 文件
     * @param genChartByAiRequest 智能分析请求体
     * @param request 网络请求体
     * @return 操作响应体
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String goal = genChartByAiRequest.getGoal();
        String name = genChartByAiRequest.getName();
        String chartType = genChartByAiRequest.getChartType();

        //校验
        // 如果分析目标为空，抛出异常，并提示
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标为空");
        // 如果图像名称为空或过长 > 100，抛出异常，并提示
        ThrowUtils.throwIf(StringUtils.isBlank(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "图像名称为空或过长");

        // 文件校验 region
        // 获取文件信息（大小、文件名）
        long fileSize = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();

        // 校验文件大小，如果大于1MB, 抛出异常，并提示
        ThrowUtils.throwIf(fileSize > FileConstant.ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1M");

        //校验文件后缀
        String fileSuffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList(".xlsx", ".xls");
        ThrowUtils.throwIf(validFileSuffixList.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型非法");
        // end 文件校验 region


        // 通过 request 拿到用户的id (需登录才能使用）
        User loginUser = userService.getLoginUser(request);

        // 限流判断，每个用户一个限流器
        redisLimiterManager.doRateLimiter("genChartByAi_" + loginUser.getId());


        // 用户输入 Prompt 的设计
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");

        // 拼接分析目标
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += "，请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");

        // 拼接数据
        userInput.append("原始数据：").append("\n");
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        // 先把图表插入数据库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setName(name);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 原始数据分表存储
        boolean createTableResult = chartService.createCsvDataTable(chart.getId(), csvData);
        ThrowUtils.throwIf(!createTableResult, ErrorCode.SYSTEM_ERROR, "原始数据保存失败");
        long newChartId = chart.getId();


        biMessageProducer.sendMessage(String.valueOf(newChartId));

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);

        return ResultUtils.success(biResponse);

    }


}


