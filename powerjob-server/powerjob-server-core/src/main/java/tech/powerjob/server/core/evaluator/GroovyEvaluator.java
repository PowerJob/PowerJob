package tech.powerjob.server.core.evaluator;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * @author Echo009
 * @since 2021/12/10
 */
@Slf4j
@Component
public class GroovyEvaluator implements Evaluator {

    private static final ScriptEngine ENGINE = new ScriptEngineManager().getEngineByName("groovy");


    @Override
    @SneakyThrows
    public Object evaluate(String expression, Object input) {
        Bindings bindings = ENGINE.createBindings();
        bindings.put("context", input);
        return ENGINE.eval(expression, bindings);
    }

}
