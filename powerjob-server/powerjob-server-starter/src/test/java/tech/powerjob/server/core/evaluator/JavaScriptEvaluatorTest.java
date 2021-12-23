package tech.powerjob.server.core.evaluator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.Assert;
import org.junit.Test;
import tech.powerjob.common.serialize.JsonUtils;

import java.util.HashMap;

/**
 * @author Echo009
 * @since 2021/12/10
 */
public class JavaScriptEvaluatorTest {

    private final JavaScriptEvaluator javaScriptEvaluator = new JavaScriptEvaluator();

    private final HashMap<String, String> SIMPLE_CONTEXT = new HashMap<>();

    private final HashMap<String, String> COMPLEX_CONTEXT = new HashMap<>();

    {
        // simple context
        // {"k1":"1","k2":"\"2\"","k3":"false","k4":"1.1"}
        SIMPLE_CONTEXT.put("k1", JsonUtils.toJSONString(1));
        SIMPLE_CONTEXT.put("k2", JsonUtils.toJSONString("2"));
        SIMPLE_CONTEXT.put("k3", JsonUtils.toJSONString(false));
        SIMPLE_CONTEXT.put("k4", JsonUtils.toJSONString(1.1d));

        // complex context
        // {"array":"[1,2,3,4,5]","obj":"{\"id\":\"e3\",\"value\":3,\"sub\":{\"id\":\"e2\",\"value\":2,\"sub\":{\"id\":\"e1\",\"value\":1,\"sub\":null}}}","map":"{\"e1\":{\"id\":\"e1\",\"value\":1,\"sub\":null}}"}
        COMPLEX_CONTEXT.put("array", JsonUtils.toJSONString(new int[]{1, 2, 3, 4, 5}));
        Element e1 = new Element("e1",1,null);
        Element e2 = new Element("e2",2,e1);
        Element e3 = new Element("e3",3,e2);
        COMPLEX_CONTEXT.put("obj", JsonUtils.toJSONString(e3));
        HashMap<String, Object> map = new HashMap<>();
        map.put("e1",e1);
        COMPLEX_CONTEXT.put("map",JsonUtils.toJSONString(map));

    }

    @Test
    public void testSimpleEval1() {
        Object res = javaScriptEvaluator.evaluate("var x = false; x;", null);
        Assert.assertEquals(false, res);
    }

    @Test
    public void testSimpleEval2() {
        Object res = javaScriptEvaluator.evaluate("var person = {name:'echo',tag:'y'}; person.name;", null);
        Assert.assertEquals("echo", res);
    }


    @Test
    public void testSimpleEval3() {
        // inject simple context
        Object res = javaScriptEvaluator.evaluate("var res = context.k3; res;", SIMPLE_CONTEXT);
        Boolean s = JsonUtils.parseObjectUnsafe(res.toString(), Boolean.class);
        Assert.assertEquals(false, s);
    }

    @Test
    public void testSimpleEval4() {
        Object res = javaScriptEvaluator.evaluate("var res = JSON.parse(context.k3); res == false;", SIMPLE_CONTEXT);
        Assert.assertEquals(true, res);
    }

    @Test
    public void testComplexEval1() {
        // array
        Object res = javaScriptEvaluator.evaluate("var res = JSON.parse(context.array); res[0] == 1;", COMPLEX_CONTEXT);
        Assert.assertEquals(true, res);
        // map
        res = javaScriptEvaluator.evaluate("var map = JSON.parse(context.map); var e1 = map.e1; e1.value ",COMPLEX_CONTEXT);
        Assert.assertEquals(1,res);
        // object
        res = javaScriptEvaluator.evaluate("var e3 = JSON.parse(context.obj); var e1 = e3.sub.sub; e1.value ",COMPLEX_CONTEXT);
        Assert.assertEquals(1,res);
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Element {

        private String id;

        private Integer value;

        private Element sub;

    }

}
