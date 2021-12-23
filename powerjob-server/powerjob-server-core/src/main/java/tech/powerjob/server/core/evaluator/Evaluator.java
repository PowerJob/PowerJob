package tech.powerjob.server.core.evaluator;


/**
 * @author Echo009
 * @since 2021/12/10
 */
public interface Evaluator {
    /**
     * 使用给定输入计算表达式
     *
     * @param expression 可执行的表达式
     * @param input      输入
     * @return 计算结果
     */
    Object evaluate(String expression, Object input);

}
