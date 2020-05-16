package com.github.kfcfans.oms.worker.container;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 容器工厂
 *
 * @author tjq
 * @since 2020/5/16
 */
public class OmsContainerFactory {

    private static final Map<Long, OmsContainer> CARGO = Maps.newConcurrentMap();

}
