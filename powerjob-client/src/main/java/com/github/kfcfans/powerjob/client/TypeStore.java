package com.github.kfcfans.powerjob.client;

import com.alibaba.fastjson.TypeReference;
import com.github.kfcfans.powerjob.common.response.*;

/**
 * TypeReference store.
 *
 * @author tjq
 * @since 11/7/20
 */
public class TypeStore {

    public static final TypeReference<ResultDTO<Void>> VOID_RESULT_TYPE = new TypeReference<ResultDTO<Void>>(){};

    public static final TypeReference<ResultDTO<Integer>> INTEGER_RESULT_TYPE = new TypeReference<ResultDTO<Integer>>(){};

    public static final TypeReference<ResultDTO<Long>> LONG_RESULT_TYPE = new TypeReference<ResultDTO<Long>>(){};

    public static final TypeReference<ResultDTO<JobInfoDTO>> JOB_RESULT_TYPE = new TypeReference<ResultDTO<JobInfoDTO>>(){};

    public static final TypeReference<ResultDTO<InstanceInfoDTO>> INSTANCE_RESULT_TYPE = new TypeReference<ResultDTO<InstanceInfoDTO>>() {};

    public static final TypeReference<ResultDTO<WorkflowInfoDTO>> WF_RESULT_TYPE = new TypeReference<ResultDTO<WorkflowInfoDTO>>() {};

    public static final TypeReference<ResultDTO<WorkflowInstanceInfoDTO>> WF_INSTANCE_RESULT_TYPE = new TypeReference<ResultDTO<WorkflowInstanceInfoDTO>>() {};

}
