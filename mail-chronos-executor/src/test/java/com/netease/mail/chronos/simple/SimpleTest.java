package com.netease.mail.chronos.simple;

import com.netease.mail.chronos.executor.serviceupgrade.util.SignUtils;
import org.junit.Test;

import java.util.HashMap;

/**
 * @author Echo009
 * @since 2022/1/7
 */
public class SimpleTest {

    @Test
    public void testGenSign() {

        HashMap<String, String> params = new HashMap<>();
        params.put("product", "MAIL_CHRONOS");
        params.put("resource", "FC_CAPACITY");
        params.put("strategy", "11");
        params.put("extInfo", "null");
        params.put("account", "ce24965c5b1a9017038b59c6c1ac0f03");
        params.put("token", "796782301735292928");

        System.out.println(SignUtils.md5Sign(params, "ezuUEA_4tfOze8JYjlY3cB1r"));
        // 0ED06C0480A50780A503308F07E0EE03400E02E02606E0A5
        // 0A808302C08501102600E04A0940110910710010740E6058
        // [cmd:md5,paramSource:account=ce24965c5b1a9017038b59c6c1ac0f03&product=MAIL_CHRONOS&resource=FC_CAPACITY&strategy=11&token=796782301735292928&,sign:0ED06C0480A50780A503308F07E0EE03400E02E02606E0A5]
        // [cmd:md5,paramSource:account=ce24965c5b1a9017038b59c6c1ac0f03&extInfo=null&product=MAIL_CHRONOS&resource=FC_CAPACITY&strategy=11&token=796782301735292928&,sign:0A808302C08501102600E04A0940110910710010740E6058]
    }

}
